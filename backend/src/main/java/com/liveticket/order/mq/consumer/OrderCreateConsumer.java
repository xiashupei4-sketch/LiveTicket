package com.liveticket.order.mq.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.constant.RabbitConstants;
import com.liveticket.order.entity.MqConsumeLog;
import com.liveticket.order.mapper.MqConsumeLogMapper;
import com.liveticket.order.mq.message.OrderCreateMessage;
import com.liveticket.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单创建消费者。
 * 链路：接收消息 → Redisson lock → consume_log 幂等 → @Transactional 落库 → Redis result=SUCCESS:{orderNo} → ACK。
 * 单条消息最多处理 3 次（500ms / 1000ms 退避），最终失败 basicReject(requeue=false) 进入 DLX。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private static final int MAX_ATTEMPTS = 3;
    private static final String BUSINESS_TYPE = "ORDER_CREATE";
    private static final Duration RESULT_TTL = Duration.ofSeconds(1800);

    private final OrderService orderService;
    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConstants.ORDER_CREATE_QUEUE)
    public void onMessage(OrderCreateMessage message, Channel channel,
                          @org.springframework.messaging.handler.annotation.Header(AmqpHeaders.DELIVERY_TAG)
                          long deliveryTag) throws IOException {
        String messageId = message.getMessageId();
        String orderNo = message.getOrderNo();
        log.info("[MQ_ORDER] consume start messageId={} orderNo={} userId={} skuId={}",
                messageId, orderNo, message.getUserId(), message.getTicketSkuId());

        String lockKey = RedisKeys.lockOrder(message.getUserId(), message.getTicketSkuId());
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!locked) {
            handleFinalFailure(message, channel, deliveryTag, "ORDER_PROCESSING_CONFLICT");
            return;
        }

        try {
            MqConsumeLog existLog = mqConsumeLogMapper.selectOne(Wrappers.<MqConsumeLog>lambdaQuery()
                    .eq(MqConsumeLog::getMessageId, messageId));
            if (existLog != null && existLog.getConsumeStatus() != null
                    && existLog.getConsumeStatus() == MqConsumeLog.STATUS_SUCCESS) {
                // 幂等：重复 messageId 不创建重复订单
                log.info("[MQ_ORDER] duplicate messageId={} already SUCCESS, ack only", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (existLog == null) {
                MqConsumeLog consumeLog = new MqConsumeLog();
                consumeLog.setMessageId(messageId);
                consumeLog.setBusinessType(BUSINESS_TYPE);
                consumeLog.setBusinessId(orderNo);
                consumeLog.setConsumeStatus(MqConsumeLog.STATUS_PROCESSING);
                consumeLog.setRetryCount(0);
                try {
                    mqConsumeLogMapper.insert(consumeLog);
                } catch (org.springframework.dao.DuplicateKeyException e) {
                    // 并发重复消息：已有处理记录，直接 ACK
                    log.info("[MQ_ORDER] concurrent duplicate messageId={}, ack only", messageId);
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                try {
                    log.info("[MQ_ORDER] messageId={} orderNo={} attempt={}", messageId, orderNo, attempt);
                    orderService.createSeckillOrder(message);

                    String resultKey = RedisKeys.seckillResult(message.getUserId(), message.getTicketSkuId());
                    stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS:" + orderNo, RESULT_TTL);
                    log.info("[SECKILL] requestId={} userId={} skuId={} result=SUCCESS orderNo={}",
                            messageId, message.getUserId(), message.getTicketSkuId(), orderNo);

                    channel.basicAck(deliveryTag, false);
                    return;
                } catch (Exception e) {
                    mqConsumeLogMapper.updateRetry(messageId, attempt, abbreviate(e.getMessage()));
                    log.warn("[MQ_ORDER] attempt failed messageId={} attempt={} error={}",
                            messageId, attempt, e.getMessage());
                    if (attempt < MAX_ATTEMPTS) {
                        backoff(attempt == 1 ? 500L : 1000L);
                    }
                }
            }

            handleFinalFailure(message, channel, deliveryTag, "MAX_ATTEMPTS_EXCEEDED");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    private void handleFinalFailure(OrderCreateMessage message, Channel channel, long deliveryTag, String reason)
            throws IOException {
        log.error("[MQ_ORDER] final failure messageId={} orderNo={} reason={} -> DLX",
                message.getMessageId(), message.getOrderNo(), reason);
        mqConsumeLogMapper.updateRetry(message.getMessageId(), MAX_ATTEMPTS, reason);
        channel.basicReject(deliveryTag, false);
    }

    private void backoff(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("consumer backoff interrupted", e);
        }
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 450 ? message.substring(0, 450) : message;
    }
}

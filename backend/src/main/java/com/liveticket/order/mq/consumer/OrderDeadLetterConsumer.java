package com.liveticket.order.mq.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.common.constant.RabbitConstants;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.order.entity.MqConsumeLog;
import com.liveticket.order.mapper.MqConsumeLogMapper;
import com.liveticket.order.mq.message.OrderCreateMessage;
import com.liveticket.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * 死信消费者：主队列 3 次处理失败后进入 DLQ。
 * 已存在成功订单 → 仅 ACK；否则执行 Redis 库存与抢票资格补偿并标记 consume_log 失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadLetterConsumer {

    private static final Duration RESULT_TTL = Duration.ofSeconds(1800);

    private final OrderService orderService;
    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConstants.ORDER_DLX_QUEUE)
    public void onMessage(OrderCreateMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String messageId = message.getMessageId();
        log.warn("[MQ_ORDER] DLQ receive messageId={} orderNo={} userId={} skuId={}",
                messageId, message.getOrderNo(), message.getUserId(), message.getTicketSkuId());

        // 1. 查询是否已经存在成功订单
        if (orderService.hasActiveSeckillOrder(message.getUserId(), message.getTicketSkuId())) {
            log.info("[MQ_ORDER] DLQ skip, active order already exists messageId={}", messageId);
            channel.basicAck(deliveryTag, false);
            return;
        }

        // 2. 补偿：Redis 库存恢复 + 抢票资格恢复 + 结果置 FAILED
        String stockKey = RedisKeys.seckillStock(message.getTicketSkuId());
        String userKey = RedisKeys.seckillUsers(message.getTicketSkuId());
        String resultKey = RedisKeys.seckillResult(message.getUserId(), message.getTicketSkuId());
        int quantity = message.getQuantity() == null ? 1 : message.getQuantity();
        stringRedisTemplate.opsForValue().increment(stockKey, quantity);
        stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(message.getUserId()));
        stringRedisTemplate.opsForValue().set(resultKey, "FAILED:ORDER_CREATE_FAILED", RESULT_TTL);

        // 3. consume_log 标记最终失败
        MqConsumeLog logRow = mqConsumeLogMapper.selectOne(Wrappers.<MqConsumeLog>lambdaQuery()
                .eq(MqConsumeLog::getMessageId, messageId));
        if (logRow != null) {
            logRow.setConsumeStatus(MqConsumeLog.STATUS_FAILED);
            logRow.setLastError("DLQ_COMPENSATED");
            mqConsumeLogMapper.updateById(logRow);
        }

        log.warn("[SECKILL] userId={} skuId={} result=ORDER_CREATE_FAILED compensatedBy=DLQ",
                message.getUserId(), message.getTicketSkuId());
        channel.basicAck(deliveryTag, false);
    }
}

package com.liveticket.order.mq.producer;

import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.constant.RabbitConstants;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.order.mq.message.OrderCreateMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秒杀订单消息生产者。
 * Publisher Confirm / Return 失败时执行补偿：Redis 库存 +1、移除已抢用户、结果置 FAILED:MQ_SEND_FAILED。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageProducer implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /** 等待 Broker Confirm 的消息（内存 outbox，Confirm 成功后移除） */
    protected final ConcurrentHashMap<String, OrderCreateMessage> pending = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    public void send(OrderCreateMessage message) {
        pending.put(message.getMessageId(), message);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConstants.ORDER_EXCHANGE,
                    RabbitConstants.ORDER_CREATE_ROUTING_KEY,
                    message,
                    m -> {
                        m.getMessageProperties().setMessageId(message.getMessageId());
                        m.getMessageProperties().setTimestamp(new java.util.Date(message.getTimestamp()));
                        return m;
                    },
                    new CorrelationData(message.getMessageId()));
            log.info("[MQ_ORDER] producer send messageId={} orderNo={} userId={} skuId={}",
                    message.getMessageId(), message.getOrderNo(), message.getUserId(), message.getTicketSkuId());
        } catch (AmqpException e) {
            pending.remove(message.getMessageId());
            log.error("[MQ_ORDER] producer send failed messageId={} cause={}", message.getMessageId(), e.getMessage());
            compensate(message, e.getMessage());
            throw new BusinessException(ErrorCode.MQ_SEND_FAILED);
        }
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null || correlationData.getId() == null) {
            return;
        }
        String messageId = correlationData.getId();
        if (ack) {
            OrderCreateMessage removed = pending.remove(messageId);
            log.info("[MQ_ORDER] confirm ack messageId={} pendingRemoved={}", messageId, removed != null);
            return;
        }
        OrderCreateMessage message = pending.remove(messageId);
        if (message != null) {
            log.error("[MQ_ORDER] confirm nack messageId={} cause={}", messageId, cause);
            compensate(message, "BROKER_NACK: " + cause);
        }
    }

    @Override
    public void returnedMessage(org.springframework.amqp.core.ReturnedMessage returned) {
        if (returned.getMessage() == null
                || returned.getMessage().getMessageProperties().getMessageId() == null) {
            return;
        }
        String messageId = returned.getMessage().getMessageProperties().getMessageId();
        OrderCreateMessage message = pending.remove(messageId);
        if (message != null) {
            log.error("[MQ_ORDER] message returned messageId={} replyText={}", messageId, returned.getReplyText());
            compensate(message, "RETURNED: " + returned.getReplyText());
        }
    }

    protected void compensate(OrderCreateMessage message, String reason) {
        String stockKey = RedisKeys.seckillStock(message.getTicketSkuId());
        String userKey = RedisKeys.seckillUsers(message.getTicketSkuId());
        String resultKey = RedisKeys.seckillResult(message.getUserId(), message.getTicketSkuId());
        try {
            stringRedisTemplate.opsForValue().increment(stockKey, message.getQuantity());
            stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(message.getUserId()));
            stringRedisTemplate.opsForValue().set(resultKey, "FAILED:MQ_SEND_FAILED", Duration.ofSeconds(1800));
            log.warn("[SECKILL] userId={} skuId={} result=MQ_SEND_FAILED reason={}",
                    message.getUserId(), message.getTicketSkuId(), reason);
        } catch (Exception e) {
            log.error("[MQ_ORDER] compensate failed messageId={}", message.getMessageId(), e);
        }
    }
}

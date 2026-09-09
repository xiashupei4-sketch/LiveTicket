package com.liveticket.order.mq.consumer;

import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.order.entity.MqConsumeLog;
import com.liveticket.order.mapper.MqConsumeLogMapper;
import com.liveticket.order.mq.message.OrderCreateMessage;
import com.liveticket.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCreateConsumerTest {

    private OrderService orderService;
    private MqConsumeLogMapper mqConsumeLogMapper;
    private RedissonClient redissonClient;
    private RLock lock;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SetOperations<String, String> setOperations;
    private Channel channel;
    private OrderCreateConsumer consumer;
    private OrderDeadLetterConsumer deadLetterConsumer;

    private static final Long USER_ID = 10001L;
    private static final Long SKU_ID = 20001L;
    private static final String MESSAGE_ID = "8c27876d-0a1e-4c2d-b7f9-80bbd5cf8f75";
    private static final String ORDER_NO = "LT202609081523451234";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderService = mock(OrderService.class);
        mqConsumeLogMapper = mock(MqConsumeLogMapper.class);
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        channel = mock(Channel.class);
        consumer = new OrderCreateConsumer(orderService, mqConsumeLogMapper, redissonClient, stringRedisTemplate);
        deadLetterConsumer = new OrderDeadLetterConsumer(orderService, mqConsumeLogMapper, stringRedisTemplate);
    }

    private OrderCreateMessage message() {
        return new OrderCreateMessage(MESSAGE_ID, ORDER_NO, USER_ID, 10001L, SKU_ID, 1,
                System.currentTimeMillis());
    }

    private MqConsumeLog successLog() {
        MqConsumeLog log = new MqConsumeLog();
        log.setMessageId(MESSAGE_ID);
        log.setBusinessType("ORDER_CREATE");
        log.setBusinessId(ORDER_NO);
        log.setConsumeStatus(MqConsumeLog.STATUS_SUCCESS);
        log.setRetryCount(0);
        return log;
    }

    @Test
    void consumeSuccessCreatesOrderAndAcks() throws Exception {
        when(mqConsumeLogMapper.selectOne(any())).thenReturn(null);
        when(mqConsumeLogMapper.insert(any(MqConsumeLog.class))).thenReturn(1);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        doNothing().when(orderService).createSeckillOrder(any(OrderCreateMessage.class));

        consumer.onMessage(message(), channel, 1L);

        verify(orderService).createSeckillOrder(any(OrderCreateMessage.class));
        verify(valueOperations).set(eq("lt:seckill:result:" + USER_ID + ":" + SKU_ID),
                eq("SUCCESS:" + ORDER_NO), any(java.time.Duration.class));
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    void duplicateMessageIdDoesNotCreateDuplicateOrder() throws Exception {
        when(mqConsumeLogMapper.selectOne(any())).thenReturn(successLog());
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);

        consumer.onMessage(message(), channel, 2L);

        verify(orderService, never()).createSeckillOrder(any());
        verify(mqConsumeLogMapper, never()).insert(any(MqConsumeLog.class));
        verify(channel).basicAck(2L, false);
    }

    @Test
    void threeAttemptsThenRejectToDlq() throws Exception {
        when(mqConsumeLogMapper.selectOne(any())).thenReturn(null);
        when(mqConsumeLogMapper.insert(any(MqConsumeLog.class))).thenReturn(1);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        doThrow(new BusinessException(ErrorCode.TICKET_OUT_OF_STOCK))
                .when(orderService).createSeckillOrder(any(OrderCreateMessage.class));

        consumer.onMessage(message(), channel, 3L);

        verify(orderService, times(3)).createSeckillOrder(any(OrderCreateMessage.class));
        verify(mqConsumeLogMapper, times(3)).updateRetry(eq(MESSAGE_ID), anyInt(), startsWith("ticket"));
        verify(channel).basicReject(3L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(valueOperations, never()).set(startsWith("lt:seckill:result:"), startsWith("SUCCESS:"),
                any(java.time.Duration.class));
    }

    @Test
    void dlqCompensatesStockAndUserEligibility() throws Exception {
        when(orderService.hasActiveSeckillOrder(USER_ID, SKU_ID)).thenReturn(false);
        when(mqConsumeLogMapper.selectOne(any())).thenReturn(successLog());

        deadLetterConsumer.onMessage(message(), channel, 4L);

        verify(valueOperations).increment("lt:seckill:stock:" + SKU_ID, 1L);
        verify(setOperations).remove("lt:seckill:users:" + SKU_ID, String.valueOf(USER_ID));
        verify(valueOperations).set(eq("lt:seckill:result:" + USER_ID + ":" + SKU_ID),
                eq("FAILED:ORDER_CREATE_FAILED"), any(java.time.Duration.class));
        ArgumentCaptor<MqConsumeLog> captor = ArgumentCaptor.forClass(MqConsumeLog.class);
        verify(mqConsumeLogMapper).updateById(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getConsumeStatus())
                .isEqualTo(MqConsumeLog.STATUS_FAILED);
        verify(channel).basicAck(4L, false);
    }

    @Test
    void dlqSkipsCompensationWhenSuccessOrderExists() throws Exception {
        when(orderService.hasActiveSeckillOrder(USER_ID, SKU_ID)).thenReturn(true);

        deadLetterConsumer.onMessage(message(), channel, 5L);

        verify(valueOperations, never()).increment(anyString(), anyLong());
        verify(setOperations, never()).remove(anyString(), anyString());
        verify(mqConsumeLogMapper, never()).updateById(any(MqConsumeLog.class));
        verify(channel).basicAck(5L, false);
    }

    @Test
    void producerSendFailureCompensatesImmediately() {
        OrderMessageProducerForTest producer = new OrderMessageProducerForTest(stringRedisTemplate);
        OrderCreateMessage msg = message();

        producer.compensatePublicly(msg, "connection refused");

        verify(valueOperations).increment("lt:seckill:stock:" + SKU_ID, 1L);
        verify(setOperations).remove("lt:seckill:users:" + SKU_ID, String.valueOf(USER_ID));
        verify(valueOperations).set(eq("lt:seckill:result:" + USER_ID + ":" + SKU_ID),
                eq("FAILED:MQ_SEND_FAILED"), any(java.time.Duration.class));
    }

    @Test
    void producerConfirmNackTriggersCompensation() {
        OrderMessageProducerForTest producer = new OrderMessageProducerForTest(stringRedisTemplate);
        OrderCreateMessage msg = message();
        producer.putPending(MESSAGE_ID, msg);

        producer.confirm(new CorrelationData(MESSAGE_ID), false, "channel closed");

        verify(valueOperations).increment("lt:seckill:stock:" + SKU_ID, 1L);
        verify(setOperations).remove("lt:seckill:users:" + SKU_ID, String.valueOf(USER_ID));
        verify(valueOperations).set(startsWith("lt:seckill:result:"), eq("FAILED:MQ_SEND_FAILED"),
                any(java.time.Duration.class));
    }

    @Test
    void producerReturnTriggersCompensation() {
        OrderMessageProducerForTest producer = new OrderMessageProducerForTest(stringRedisTemplate);
        OrderCreateMessage msg = message();
        producer.putPending(MESSAGE_ID, msg);

        org.springframework.amqp.core.Message amqpMessage = new org.springframework.amqp.core.Message(
                "{}".getBytes(StandardCharsets.UTF_8),
                messagePropsWithId(MESSAGE_ID));
        producer.returnedMessage(new ReturnedMessage(amqpMessage, 312, "NO_ROUTE",
                "exchange", "routingKey"));

        verify(valueOperations).increment("lt:seckill:stock:" + SKU_ID, 1L);
        verify(valueOperations).set(startsWith("lt:seckill:result:"), eq("FAILED:MQ_SEND_FAILED"),
                any(java.time.Duration.class));
    }

    private org.springframework.amqp.core.MessageProperties messagePropsWithId(String messageId) {
        org.springframework.amqp.core.MessageProperties props = new org.springframework.amqp.core.MessageProperties();
        props.setMessageId(messageId);
        return props;
    }

    /**
     * 测试可见性桥接：绕过 @PostConstruct（无运行中的 RabbitTemplate）
     */
    static class OrderMessageProducerForTest extends com.liveticket.order.mq.producer.OrderMessageProducer {
        OrderMessageProducerForTest(StringRedisTemplate stringRedisTemplate) {
            super(null, stringRedisTemplate);
        }

        void putPending(String messageId, OrderCreateMessage message) {
            super.pending.put(messageId, message);
        }

        void compensatePublicly(OrderCreateMessage message, String reason) {
            super.compensate(message, reason);
        }
    }
}

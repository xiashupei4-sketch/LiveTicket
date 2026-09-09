package com.liveticket.seckill.service;

import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.seckill.service.impl.SeckillServiceImpl;
import com.liveticket.seckill.vo.SeckillResultVO;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TicketSkuMapper ticketSkuMapper;
    private EventMapper eventMapper;
    private SeckillServiceImpl seckillService;

    private static final Long USER_ID = 10001L;
    private static final Long SKU_ID = 4L;
    private static final Long EVENT_ID = 2L;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        ticketSkuMapper = mock(TicketSkuMapper.class);
        eventMapper = mock(EventMapper.class);

        seckillService = new SeckillServiceImpl(stringRedisTemplate, ticketSkuMapper, eventMapper,
                mock(com.liveticket.order.mq.producer.OrderMessageProducer.class));
        var field = SeckillServiceImpl.class.getDeclaredField("seckillScript");
        field.setAccessible(true);
        field.set(seckillService, mock(RedisScript.class));
    }

    private TicketSku onSaleSku() {
        TicketSku sku = new TicketSku();
        sku.setId(SKU_ID);
        sku.setEventId(EVENT_ID);
        sku.setSkuName("前排票");
        sku.setPrice(new java.math.BigDecimal("380.00"));
        sku.setTotalStock(5);
        sku.setAvailableStock(5);
        sku.setPerUserLimit(1);
        sku.setStatus(1);
        return sku;
    }

    private Event onSaleEvent() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Midnight Echo");
        event.setStatus(1);
        event.setSaleStartTime(LocalDateTime.now().minusDays(1));
        event.setSaleEndTime(LocalDateTime.now().plusDays(10));
        return event;
    }

    @Test
    void luaSuccessSetsProcessingResult() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), eq(String.valueOf(USER_ID))))
                .thenReturn(0L);

        SeckillResultVO result = seckillService.executeSeckill(USER_ID, SKU_ID);

        assertThat(result.getStatus()).isEqualTo("PROCESSING");
        verify(stringRedisTemplate).execute(any(RedisScript.class),
                argThat((List<String> keys) -> keys.get(0).equals("lt:seckill:stock:" + SKU_ID)
                        && keys.get(1).equals("lt:seckill:users:" + SKU_ID)),
                eq(String.valueOf(USER_ID)));
        verify(valueOperations).set(eq("lt:seckill:result:" + USER_ID + ":" + SKU_ID),
                eq("PROCESSING"), any(java.time.Duration.class));
    }

    @Test
    void luaOutOfStockRejected() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, SKU_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.SECKILL_OUT_OF_STOCK.getCode());
        // 库存不足时不得写入抢票结果
        verify(valueOperations, never()).set(anyString(), anyString(), any(java.time.Duration.class));
    }

    @Test
    void duplicateUserRejected() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(2L);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, SKU_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.DUPLICATE_PURCHASE.getCode());
        verify(valueOperations, never()).set(anyString(), anyString(), any(java.time.Duration.class));
    }

    @Test
    void saleNotStartedRejected() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        Event event = onSaleEvent();
        event.setSaleStartTime(LocalDateTime.now().plusHours(1));
        when(eventMapper.selectById(EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, SKU_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.SECKILL_NOT_STARTED.getCode());
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString());
    }

    @Test
    void saleEndedRejected() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        Event event = onSaleEvent();
        event.setSaleEndTime(LocalDateTime.now().minusMinutes(1));
        when(eventMapper.selectById(EVENT_ID)).thenReturn(event);

        assertThatThrownBy(() -> seckillService.executeSeckill(USER_ID, SKU_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.SECKILL_ENDED.getCode());
    }

    @Test
    void resultParsing() {
        ValueOperations<String, String> ops = valueOperations;
        when(ops.get("lt:seckill:result:" + USER_ID + ":" + SKU_ID))
                .thenReturn(null, "PROCESSING", "SUCCESS:LT202609081523451234", "FAILED:ORDER_CREATE_FAILED");

        assertThat(seckillService.getResult(USER_ID, SKU_ID).getStatus()).isEqualTo("PROCESSING");

        SeckillResultVO processing = seckillService.getResult(USER_ID, SKU_ID);
        assertThat(processing.getStatus()).isEqualTo("PROCESSING");
        assertThat(processing.getOrderNo()).isNull();

        SeckillResultVO success = seckillService.getResult(USER_ID, SKU_ID);
        assertThat(success.getStatus()).isEqualTo("SUCCESS");
        assertThat(success.getOrderNo()).isEqualTo("LT202609081523451234");

        SeckillResultVO failed = seckillService.getResult(USER_ID, SKU_ID);
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getReason()).isEqualTo("ORDER_CREATE_FAILED");
    }
}

package com.liveticket.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.order.dto.CreateOrderRequest;
import com.liveticket.order.entity.Order;
import com.liveticket.order.mapper.OrderMapper;
import com.liveticket.order.service.impl.OrderServiceImpl;
import com.liveticket.order.vo.OrderVO;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderMapper orderMapper;
    private TicketSkuMapper ticketSkuMapper;
    private EventMapper eventMapper;
    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SetOperations<String, String> setOperations;
    private OrderServiceImpl orderService;

    private static final Long USER_ID = 100L;
    private static final Long SKU_ID = 5L;
    private static final Long EVENT_ID = 2L;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        ticketSkuMapper = mock(TicketSkuMapper.class);
        eventMapper = mock(EventMapper.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);

        orderService = new OrderServiceImpl(orderMapper, ticketSkuMapper, eventMapper, stringRedisTemplate,
                mock(com.liveticket.order.mapper.MqConsumeLogMapper.class));
    }

    private TicketSku onSaleSku() {
        TicketSku sku = new TicketSku();
        sku.setId(SKU_ID);
        sku.setEventId(EVENT_ID);
        sku.setSkuName("标准票");
        sku.setPrice(new BigDecimal("280.00"));
        sku.setTotalStock(100);
        sku.setAvailableStock(100);
        sku.setPerUserLimit(4);
        sku.setStatus(1);
        return sku;
    }

    private Event onSaleEvent() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Midnight Echo");
        event.setStatus(1);
        return event;
    }

    private CreateOrderRequest createRequest(int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setTicketSkuId(SKU_ID);
        request.setQuantity(quantity);
        return request;
    }

    @Test
    void normalOrderSuccessDeductsStock() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(ticketSkuMapper.deductStock(SKU_ID, 2)).thenReturn(1);
        when(orderMapper.insert(any(Order.class))).thenReturn(1);

        OrderVO vo = orderService.createNormalOrder(USER_ID, createRequest(2));

        assertThat(vo.getStatus()).isEqualTo(0);
        assertThat(vo.getSource()).isEqualTo(0);
        assertThat(vo.getQuantity()).isEqualTo(2);
        assertThat(vo.getTotalAmount()).isEqualByComparingTo(new BigDecimal("560.00"));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(captor.capture());
        Order inserted = captor.getValue();
        assertThat(inserted.getPurchaseGuard()).isEqualTo(USER_ID + ":" + SKU_ID);
        assertThat(inserted.getOrderNo()).startsWith("LT");
        verify(ticketSkuMapper).deductStock(SKU_ID, 2);
    }

    @Test
    void outOfStockRejected() {
        TicketSku sku = onSaleSku();
        sku.setAvailableStock(1);
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(sku);
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(ticketSkuMapper.deductStock(SKU_ID, 2)).thenReturn(0);

        assertThatThrownBy(() -> orderService.createNormalOrder(USER_ID, createRequest(2)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.TICKET_OUT_OF_STOCK.getCode());
    }

    @Test
    void duplicateActiveOrderRejected() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(orderMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> orderService.createNormalOrder(USER_ID, createRequest(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.ORDER_ALREADY_EXISTS.getCode());
        verify(ticketSkuMapper, never()).deductStock(anyLong(), anyInt());
    }

    @Test
    void concurrentDuplicateGuardRollsBack() {
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(ticketSkuMapper.deductStock(SKU_ID, 1)).thenReturn(1);
        when(orderMapper.insert(any(Order.class))).thenThrow(new DuplicateKeyException("uk_purchase_guard"));

        assertThatThrownBy(() -> orderService.createNormalOrder(USER_ID, createRequest(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.ORDER_ALREADY_EXISTS.getCode());
    }

    @Test
    void cancelRestoresStockAndClearsGuard() {
        Order pending = new Order();
        pending.setOrderNo("LT1");
        pending.setUserId(USER_ID);
        pending.setEventId(EVENT_ID);
        pending.setTicketSkuId(SKU_ID);
        pending.setQuantity(2);
        pending.setUnitPrice(new BigDecimal("280.00"));
        pending.setTotalAmount(new BigDecimal("560.00"));
        pending.setStatus(0);
        pending.setSource(0);
        pending.setPurchaseGuard(USER_ID + ":" + SKU_ID);

        Order cancelled = new Order();
        cancelled.setOrderNo("LT1");
        cancelled.setUserId(USER_ID);
        cancelled.setEventId(EVENT_ID);
        cancelled.setTicketSkuId(SKU_ID);
        cancelled.setQuantity(2);
        cancelled.setUnitPrice(new BigDecimal("280.00"));
        cancelled.setTotalAmount(new BigDecimal("560.00"));
        cancelled.setStatus(2);
        cancelled.setSource(0);

        when(orderMapper.selectOne(any())).thenReturn(pending, cancelled);
        when(orderMapper.markCancelled("LT1", USER_ID)).thenReturn(1);
        when(ticketSkuMapper.restoreStock(SKU_ID, 2)).thenReturn(1);
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());

        OrderVO vo = orderService.cancel(USER_ID, "LT1");

        assertThat(vo.getStatus()).isEqualTo(2);
        verify(ticketSkuMapper).restoreStock(SKU_ID, 2);
        // 普通订单不触碰 Redis 秒杀库存
        verify(valueOperations, never()).increment(anyString(), anyLong());
    }

    @Test
    void cancelSeckillOrderRestoresRedisStock() {
        Order seckillPending = new Order();
        seckillPending.setOrderNo("LT2");
        seckillPending.setUserId(USER_ID);
        seckillPending.setEventId(EVENT_ID);
        seckillPending.setTicketSkuId(SKU_ID);
        seckillPending.setQuantity(1);
        seckillPending.setUnitPrice(new BigDecimal("380.00"));
        seckillPending.setTotalAmount(new BigDecimal("380.00"));
        seckillPending.setStatus(0);
        seckillPending.setSource(1);
        seckillPending.setPurchaseGuard(USER_ID + ":" + SKU_ID);

        Order seckillCancelled = new Order();
        seckillCancelled.setOrderNo("LT2");
        seckillCancelled.setUserId(USER_ID);
        seckillCancelled.setEventId(EVENT_ID);
        seckillCancelled.setTicketSkuId(SKU_ID);
        seckillCancelled.setQuantity(1);
        seckillCancelled.setUnitPrice(new BigDecimal("380.00"));
        seckillCancelled.setTotalAmount(new BigDecimal("380.00"));
        seckillCancelled.setStatus(2);
        seckillCancelled.setSource(1);

        when(orderMapper.selectOne(any())).thenReturn(seckillPending, seckillCancelled);
        when(orderMapper.markCancelled("LT2", USER_ID)).thenReturn(1);
        when(ticketSkuMapper.restoreStock(SKU_ID, 1)).thenReturn(1);
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());

        OrderVO vo = orderService.cancel(USER_ID, "LT2");

        assertThat(vo.getStatus()).isEqualTo(2);
        verify(valueOperations).increment(eq("lt:seckill:stock:" + SKU_ID), eq(1L));
        verify(setOperations).remove(eq("lt:seckill:users:" + SKU_ID), eq(String.valueOf(USER_ID)));
    }

    @Test
    void paidOrderCannotBeCancelled() {
        Order paid = new Order();
        paid.setOrderNo("LT3");
        paid.setUserId(USER_ID);
        paid.setTicketSkuId(SKU_ID);
        paid.setEventId(EVENT_ID);
        paid.setQuantity(1);
        paid.setStatus(1);

        when(orderMapper.selectOne(any())).thenReturn(paid);

        assertThatThrownBy(() -> orderService.cancel(USER_ID, "LT3"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_INVALID.getCode());
        verify(orderMapper, never()).markCancelled(anyString(), anyLong());
    }

    @Test
    void payTransitionsPendingToPaid() {
        Order pending = new Order();
        pending.setOrderNo("LT4");
        pending.setUserId(USER_ID);
        pending.setEventId(EVENT_ID);
        pending.setTicketSkuId(SKU_ID);
        pending.setQuantity(1);
        pending.setUnitPrice(new BigDecimal("280.00"));
        pending.setStatus(0);
        pending.setSource(0);

        Order paid = new Order();
        paid.setOrderNo("LT4");
        paid.setUserId(USER_ID);
        paid.setEventId(EVENT_ID);
        paid.setTicketSkuId(SKU_ID);
        paid.setQuantity(1);
        paid.setStatus(1);
        paid.setSource(0);

        when(orderMapper.selectOne(any())).thenReturn(pending, paid);
        when(orderMapper.markPaid("LT4", USER_ID)).thenReturn(1);
        when(ticketSkuMapper.selectById(SKU_ID)).thenReturn(onSaleSku());
        when(eventMapper.selectById(EVENT_ID)).thenReturn(onSaleEvent());

        OrderVO vo = orderService.pay(USER_ID, "LT4");

        assertThat(vo.getStatus()).isEqualTo(1);
        verify(orderMapper).markPaid("LT4", USER_ID);
    }

    @Test
    void listOrdersReturnsEnrichedPage() {
        Order order = new Order();
        order.setOrderNo("LT5");
        order.setUserId(USER_ID);
        order.setEventId(EVENT_ID);
        order.setTicketSkuId(SKU_ID);
        order.setQuantity(1);
        order.setUnitPrice(new BigDecimal("280.00"));
        order.setTotalAmount(new BigDecimal("280.00"));
        order.setStatus(0);
        order.setSource(0);

        Page<Order> result = pagedResult(order);
        org.mockito.Mockito.doReturn(result).when(orderMapper).selectPage(any(), any());
        when(eventMapper.selectBatchIds(any())).thenReturn(List.of(onSaleEvent()));
        when(ticketSkuMapper.selectBatchIds(any())).thenReturn(List.of(onSaleSku()));

        PageResponse<OrderVO> page = orderService.listUserOrders(USER_ID, null, 1, 20);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords().get(0).getEventTitle()).isEqualTo("Midnight Echo");
        assertThat(page.getRecords().get(0).getSkuName()).isEqualTo("标准票");
    }

    private Page<Order> pagedResult(Order order) {
        Page<Order> page = new Page<>(1, 20);
        page.setRecords(List.of(order));
        page.setTotal(1);
        return page;
    }
}

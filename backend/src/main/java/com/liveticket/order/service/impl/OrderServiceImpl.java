package com.liveticket.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.liveticket.common.api.PageResponse;
import com.liveticket.common.context.UserContext;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.enums.EventStatus;
import com.liveticket.common.enums.OrderSource;
import com.liveticket.common.enums.OrderStatus;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.common.util.OrderNoGenerator;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.order.dto.CreateOrderRequest;
import com.liveticket.order.entity.MqConsumeLog;
import com.liveticket.order.entity.Order;
import com.liveticket.order.mapper.MqConsumeLogMapper;
import com.liveticket.order.mapper.OrderMapper;
import com.liveticket.order.mq.message.OrderCreateMessage;
import com.liveticket.order.service.OrderService;
import com.liveticket.order.vo.OrderVO;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final EventMapper eventMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MqConsumeLogMapper mqConsumeLogMapper;

    public OrderServiceImpl(OrderMapper orderMapper,
                            TicketSkuMapper ticketSkuMapper,
                            EventMapper eventMapper,
                            StringRedisTemplate stringRedisTemplate,
                            MqConsumeLogMapper mqConsumeLogMapper) {
        this.orderMapper = orderMapper;
        this.ticketSkuMapper = ticketSkuMapper;
        this.eventMapper = eventMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.mqConsumeLogMapper = mqConsumeLogMapper;
    }

    @Override
    @Transactional
    public OrderVO createNormalOrder(Long userId, CreateOrderRequest request) {
        Long skuId = request.getTicketSkuId();
        int quantity = request.getQuantity();

        TicketSku sku = ticketSkuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TICKET_SKU_NOT_FOUND);
        }
        if (quantity > sku.getPerUserLimit()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID,
                    "quantity exceeds per user limit " + sku.getPerUserLimit());
        }

        Event event = eventMapper.selectById(sku.getEventId());
        if (event == null) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        if (event.getStatus() == null || event.getStatus() != EventStatus.ON_SALE.getCode()) {
            throw new BusinessException(ErrorCode.EVENT_NOT_ON_SALE);
        }

        // 同一用户同一票档只允许一个有效订单（PENDING / PAID）
        Long activeCount = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getTicketSkuId, skuId)
                .in(Order::getStatus, OrderStatus.PENDING.getCode(), OrderStatus.PAID.getCode()));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        // 条件扣减库存，affectedRows != 1 表示库存不足
        int affected = ticketSkuMapper.deductStock(skuId, quantity);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.TICKET_OUT_OF_STOCK);
        }

        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.generate());
        order.setUserId(userId);
        order.setEventId(sku.getEventId());
        order.setTicketSkuId(skuId);
        order.setQuantity(quantity);
        order.setUnitPrice(sku.getPrice());
        order.setTotalAmount(sku.getPrice().multiply(new java.math.BigDecimal(quantity)));
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setSource(OrderSource.NORMAL.getCode());
        order.setPurchaseGuard(userId + ":" + skuId);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 并发下重复有效订单，事务回滚恢复已扣库存
            throw new BusinessException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        log.info("[ORDER] normal created orderNo={} userId={} skuId={} quantity={}",
                order.getOrderNo(), userId, skuId, quantity);
        return toVO(order, event, sku);
    }

    @Override
    @Transactional
    public void createSeckillOrder(OrderCreateMessage message) {
        Long userId = message.getUserId();
        Long skuId = message.getTicketSkuId();
        int quantity = message.getQuantity() == null ? 1 : message.getQuantity();

        TicketSku sku = ticketSkuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            throw new BusinessException(ErrorCode.TICKET_SKU_NOT_FOUND);
        }

        // MySQL 条件扣减库存（与 Redis 预扣对应）
        int affected = ticketSkuMapper.deductStock(skuId, quantity);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.TICKET_OUT_OF_STOCK);
        }

        Order order = new Order();
        order.setOrderNo(message.getOrderNo());
        order.setUserId(userId);
        order.setEventId(message.getEventId() != null ? message.getEventId() : sku.getEventId());
        order.setTicketSkuId(skuId);
        order.setQuantity(quantity);
        order.setUnitPrice(sku.getPrice());
        order.setTotalAmount(sku.getPrice().multiply(new java.math.BigDecimal(quantity)));
        order.setStatus(OrderStatus.PENDING.getCode());
        order.setSource(OrderSource.SECKILL.getCode());
        order.setPurchaseGuard(userId + ":" + skuId);
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // 事务回滚恢复已扣库存，由 DLQ 判断已有成功订单后决定是否补偿
            throw new BusinessException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        // 同一事务内将 consume_log 标记为 SUCCESS（规格第 18 节）
        if (message.getMessageId() != null) {
            mqConsumeLogMapper.markSuccess(message.getMessageId());
        }
        log.info("[MQ_ORDER] seckill order created orderNo={} userId={} skuId={}", message.getOrderNo(), userId, skuId);
    }

    @Override
    public boolean hasActiveSeckillOrder(Long userId, Long ticketSkuId) {
        Long count = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getTicketSkuId, ticketSkuId)
                .eq(Order::getSource, OrderSource.SECKILL.getCode())
                .in(Order::getStatus, OrderStatus.PENDING.getCode(), OrderStatus.PAID.getCode()));
        return count != null && count > 0;
    }

    @Override
    public PageResponse<OrderVO> listUserOrders(Long userId, Integer status, long page, long pageSize) {
        Page<Order> mpPage = new Page<>(page, pageSize);
        Page<Order> result = orderMapper.selectPage(mpPage, Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreatedAt));

        List<Order> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResponse.of(List.of(), result.getCurrent(), result.getSize(), result.getTotal());
        }

        List<Long> eventIds = records.stream().map(Order::getEventId).distinct().collect(Collectors.toList());
        List<Long> skuIds = records.stream().map(Order::getTicketSkuId).distinct().collect(Collectors.toList());
        Map<Long, Event> eventMap = eventMapper.selectBatchIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, TicketSku> skuMap = ticketSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(TicketSku::getId, s -> s));

        List<OrderVO> vos = records.stream()
                .map(order -> toVO(order, eventMap.get(order.getEventId()), skuMap.get(order.getTicketSkuId())))
                .collect(Collectors.toList());
        return PageResponse.of(vos, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public OrderVO getByOrderNo(Long userId, String orderNo) {
        Order order = getOwnedOrder(userId, orderNo);
        Event event = eventMapper.selectById(order.getEventId());
        TicketSku sku = ticketSkuMapper.selectById(order.getTicketSkuId());
        return toVO(order, event, sku);
    }

    @Override
    @Transactional
    public OrderVO pay(Long userId, String orderNo) {
        Order order = getOwnedOrder(userId, orderNo);
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        int affected = orderMapper.markPaid(orderNo, userId);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }
        log.info("[ORDER] paid orderNo={} userId={}", orderNo, userId);
        return getByOrderNo(userId, orderNo);
    }

    @Override
    @Transactional
    public OrderVO cancel(Long userId, String orderNo) {
        Order order = getOwnedOrder(userId, orderNo);
        if (order.getStatus() != OrderStatus.PENDING.getCode()) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        int affected = orderMapper.markCancelled(orderNo, userId);
        if (affected != 1) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
        }

        // MySQL 库存恢复
        ticketSkuMapper.restoreStock(order.getTicketSkuId(), order.getQuantity());

        // 秒杀订单需要恢复 Redis 预扣库存并移除已抢用户（事务提交后执行，避免回滚不一致）
        if (order.getSource() != null && order.getSource() == OrderSource.SECKILL.getCode()) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        restoreSeckillRedis(order);
                    }
                });
            } else {
                restoreSeckillRedis(order);
            }
        }

        log.info("[ORDER] cancelled orderNo={} userId={} skuId={} quantity={}",
                orderNo, userId, order.getTicketSkuId(), order.getQuantity());
        return getByOrderNo(userId, orderNo);
    }

    private void restoreSeckillRedis(Order order) {
        String stockKey = com.liveticket.common.constant.RedisKeys.seckillStock(order.getTicketSkuId());
        String userKey = com.liveticket.common.constant.RedisKeys.seckillUsers(order.getTicketSkuId());
        String resultKey = com.liveticket.common.constant.RedisKeys.seckillResult(
                order.getUserId(), order.getTicketSkuId());
        stringRedisTemplate.opsForValue().increment(stockKey, order.getQuantity());
        stringRedisTemplate.opsForSet().remove(userKey, String.valueOf(order.getUserId()));
        stringRedisTemplate.delete(resultKey);
    }

    private Order getOwnedOrder(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OrderVO toVO(Order order, Event event, TicketSku sku) {
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setEventId(order.getEventId());
        if (event != null) {
            vo.setEventTitle(event.getTitle());
            vo.setEventCoverUrl(event.getCoverUrl());
            vo.setEventStartTime(event.getStartTime());
            vo.setVenueName(event.getVenueName());
        }
        vo.setTicketSkuId(order.getTicketSkuId());
        if (sku != null) {
            vo.setSkuName(sku.getSkuName());
        }
        vo.setQuantity(order.getQuantity());
        vo.setUnitPrice(order.getUnitPrice());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setSource(order.getSource());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPaidAt(order.getPaidAt());
        vo.setCancelledAt(order.getCancelledAt());
        return vo;
    }
}

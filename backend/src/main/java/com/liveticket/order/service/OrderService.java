package com.liveticket.order.service;

import com.liveticket.common.api.PageResponse;
import com.liveticket.order.dto.CreateOrderRequest;
import com.liveticket.order.mq.message.OrderCreateMessage;
import com.liveticket.order.vo.OrderVO;

public interface OrderService {

    OrderVO createNormalOrder(Long userId, CreateOrderRequest request);

    /**
     * 秒杀订单异步落库（由 MQ 消费者调用），同一事务内完成库存扣减与订单插入
     */
    void createSeckillOrder(OrderCreateMessage message);

    /**
     * 是否已存在该用户该票档的有效秒杀订单（DLQ 补偿前的判断）
     */
    boolean hasActiveSeckillOrder(Long userId, Long ticketSkuId);

    PageResponse<OrderVO> listUserOrders(Long userId, Integer status, long page, long pageSize);

    OrderVO getByOrderNo(Long userId, String orderNo);

    OrderVO pay(Long userId, String orderNo);

    OrderVO cancel(Long userId, String orderNo);
}

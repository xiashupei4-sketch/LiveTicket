package com.liveticket.common.constant;

public final class RabbitConstants {

    public static final String ORDER_EXCHANGE = "liveticket.order.exchange";
    public static final String ORDER_CREATE_ROUTING_KEY = "order.create";
    public static final String ORDER_CREATE_QUEUE = "liveticket.order.create.queue";

    public static final String ORDER_DLX_EXCHANGE = "liveticket.order.dlx.exchange";
    public static final String ORDER_DLX_ROUTING_KEY = "order.create.dlx";
    public static final String ORDER_DLX_QUEUE = "liveticket.order.dlx.queue";

    private RabbitConstants() {
    }
}

package com.liveticket.common.enums;

public enum OrderStatus {

    PENDING(0),
    PAID(1),
    CANCELLED(2),
    CLOSED(3);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static OrderStatus of(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown order status: " + code);
    }
}

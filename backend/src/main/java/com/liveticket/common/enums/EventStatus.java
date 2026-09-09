package com.liveticket.common.enums;

public enum EventStatus {

    DRAFT(0),
    ON_SALE(1),
    SOLD_OUT(2),
    FINISHED(3),
    CANCELLED(4);

    private final int code;

    EventStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EventStatus of(int code) {
        for (EventStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown event status: " + code);
    }
}

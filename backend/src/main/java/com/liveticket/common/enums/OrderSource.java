package com.liveticket.common.enums;

public enum OrderSource {

    NORMAL(0),
    SECKILL(1);

    private final int code;

    OrderSource(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

package com.liveticket.common.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success", 200),

    PARAM_INVALID(10001, "param invalid", 400),
    RESOURCE_NOT_FOUND(10002, "resource not found", 404),
    REQUEST_TOO_FREQUENT(10003, "request too frequent", 400),

    USER_NOT_FOUND(20001, "user not found", 404),
    USERNAME_ALREADY_EXISTS(20002, "username already exists", 409),
    PASSWORD_INCORRECT(20003, "password incorrect", 409),

    TOKEN_MISSING(21001, "token missing", 401),
    TOKEN_INVALID(21002, "token invalid", 401),
    TOKEN_EXPIRED(21003, "token expired", 401),

    EVENT_NOT_FOUND(30001, "event not found", 404),
    EVENT_NOT_ON_SALE(30002, "event not on sale", 409),

    TICKET_SKU_NOT_FOUND(31001, "ticket sku not found", 404),
    TICKET_OUT_OF_STOCK(31002, "ticket out of stock", 409),

    SECKILL_NOT_STARTED(40001, "seckill not started", 409),
    SECKILL_ENDED(40002, "seckill ended", 409),
    SECKILL_OUT_OF_STOCK(40003, "seckill out of stock", 409),
    DUPLICATE_PURCHASE(40004, "duplicate purchase", 409),
    SECKILL_PROCESSING(40005, "seckill processing", 409),
    SECKILL_FAILED(40006, "seckill failed", 409),
    ORDER_PROCESSING_CONFLICT(40007, "order processing conflict", 409),

    ORDER_NOT_FOUND(50001, "order not found", 404),
    ORDER_STATUS_INVALID(50002, "order status invalid", 409),
    ORDER_ALREADY_EXISTS(50003, "order already exists", 409),

    FOLLOW_ALREADY_EXISTS(60001, "follow already exists", 409),
    CANNOT_FOLLOW_SELF(60002, "cannot follow self", 400),

    CHECKIN_ALREADY_EXISTS(70001, "checkin already exists", 409),

    MQ_SEND_FAILED(90001, "mq send failed", 500),
    MQ_CONSUME_FAILED(90002, "mq consume failed", 500),

    ADMIN_TOKEN_INVALID(40301, "admin token error", 403),

    INTERNAL_ERROR(99999, "internal error", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return INTERNAL_ERROR;
    }
}

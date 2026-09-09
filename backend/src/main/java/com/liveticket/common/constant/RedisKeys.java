package com.liveticket.common.constant;

public final class RedisKeys {

    private static final String PREFIX = "lt:";

    private RedisKeys() {
    }

    public static String eventDetail(Long eventId) {
        return PREFIX + "event:detail:" + eventId;
    }

    public static String lockEventRebuild(Long eventId) {
        return PREFIX + "lock:event:rebuild:" + eventId;
    }

    public static String seckillStock(Long ticketSkuId) {
        return PREFIX + "seckill:stock:" + ticketSkuId;
    }

    public static String seckillUsers(Long ticketSkuId) {
        return PREFIX + "seckill:users:" + ticketSkuId;
    }

    public static String seckillResult(Long userId, Long ticketSkuId) {
        return PREFIX + "seckill:result:" + userId + ":" + ticketSkuId;
    }

    public static String lockOrder(Long userId, Long ticketSkuId) {
        return PREFIX + "lock:order:" + userId + ":" + ticketSkuId;
    }

    public static String feed(Long userId) {
        return PREFIX + "feed:" + userId;
    }

    public static String geoEvent(String cityCode) {
        return PREFIX + "geo:event:" + cityCode;
    }

    public static String checkin(Long userId, String yyyyMM) {
        return PREFIX + "checkin:" + userId + ":" + yyyyMM;
    }

    public static String uvEvent(Long eventId, String yyyyMMdd) {
        return PREFIX + "uv:event:" + eventId + ":" + yyyyMMdd;
    }
}

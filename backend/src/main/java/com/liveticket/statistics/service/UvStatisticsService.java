package com.liveticket.statistics.service;

public interface UvStatisticsService {

    /**
     * 演出详情访问成功后 PFADD lt:uv:event:{eventId}:{yyyyMMdd} userId
     */
    void recordView(Long eventId, Long userId);

    long getUv(Long eventId, String yyyyMMdd);
}

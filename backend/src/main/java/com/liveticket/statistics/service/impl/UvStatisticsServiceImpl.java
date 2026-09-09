package com.liveticket.statistics.service.impl;

import com.liveticket.common.constant.RedisKeys;
import com.liveticket.statistics.service.UvStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class UvStatisticsServiceImpl implements UvStatisticsService {

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void recordView(Long eventId, Long userId) {
        if (userId == null) {
            return;
        }
        String key = RedisKeys.uvEvent(eventId, LocalDate.now().format(KEY_DATE));
        stringRedisTemplate.opsForHyperLogLog().add(key, String.valueOf(userId));
    }

    @Override
    public long getUv(Long eventId, String yyyyMMdd) {
        String key = RedisKeys.uvEvent(eventId, yyyyMMdd);
        Long uv = stringRedisTemplate.opsForHyperLogLog().size(key);
        return uv == null ? 0 : uv;
    }
}

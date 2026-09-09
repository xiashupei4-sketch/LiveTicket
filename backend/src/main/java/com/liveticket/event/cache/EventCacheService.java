package com.liveticket.event.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.liveticket.common.constant.RedisKeys;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.event.vo.EventDetailVO;
import com.liveticket.config.CaffeineConfig;
import com.liveticket.ticket.entity.TicketSku;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 演出详情多级缓存（严格按开发规格第 12 节算法）：
 * 1. Caffeine 命中 → 返回
 * 2. Redis 空值 "__NULL__" → EVENT_NOT_FOUND
 * 3. Redis 正常值且逻辑未过期 → 写 Caffeine → 返回
 * 4. 逻辑过期 → tryLock 重建锁 → 成功则线程池异步重建，当前请求返回旧值；失败直接返回旧值
 * 5. Redis 无 Key → 同步查 MySQL → 不存在写空值 120s 并 404；存在写 Redis + Caffeine → 返回
 */
@Slf4j
@Service
public class EventCacheService {

    public static final String NULL_VALUE = "__NULL__";
    private static final Duration NULL_VALUE_TTL = Duration.ofSeconds(120);
    private static final Duration LOGICAL_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;
    private final EventMapper eventMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final RedissonClient redissonClient;
    private final ThreadPoolExecutor eventCacheRebuildExecutor;
    private final ObjectMapper objectMapper;

    private final Cache<Object, Object> caffeineCache;

    public EventCacheService(StringRedisTemplate stringRedisTemplate,
                             EventMapper eventMapper,
                             TicketSkuMapper ticketSkuMapper,
                             RedissonClient redissonClient,
                             CacheManager cacheManager,
                             @Qualifier("eventCacheRebuildExecutor") ThreadPoolExecutor eventCacheRebuildExecutor,
                             ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.eventMapper = eventMapper;
        this.ticketSkuMapper = ticketSkuMapper;
        this.redissonClient = redissonClient;
        this.caffeineCache = (Cache<Object, Object>) cacheManager
                .getCache(CaffeineConfig.CACHE_EVENT_DETAIL).getNativeCache();
        this.eventCacheRebuildExecutor = eventCacheRebuildExecutor;
        this.objectMapper = objectMapper;
    }

    public EventDetailVO getEventDetail(Long eventId) {
        // 1. Caffeine
        Object cached = caffeineCache.getIfPresent(eventId);
        if (cached != null) {
            return (EventDetailVO) cached;
        }

        String redisKey = RedisKeys.eventDetail(eventId);
        String redisValue = stringRedisTemplate.opsForValue().get(redisKey);

        // 2. 空值缓存
        if (NULL_VALUE.equals(redisValue)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        // 3. Redis 正常值
        if (StringUtils.hasText(redisValue)) {
            EventCachePayload payload = deserialize(redisValue);
            if (payload != null && payload.getData() != null) {
                if (payload.getLogicalExpireAt() != null && payload.getLogicalExpireAt().isAfter(LocalDateTime.now())) {
                    // 逻辑未过期
                    caffeineCache.put(eventId, payload.getData());
                    return payload.getData();
                }
                // 逻辑过期：尝试获取重建锁，无论成败都返回旧值
                RLock lock = redissonClient.getLock(RedisKeys.lockEventRebuild(eventId));
                boolean locked = false;
                try {
                    locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (locked) {
                    try {
                        long currentEventId = eventId;
                        eventCacheRebuildExecutor.execute(() -> rebuildCache(currentEventId));
                    } finally {
                        // 异步重建中使用独立的锁获取，这里立即释放入口锁
                        lock.unlock();
                    }
                }
                return payload.getData();
            }
        }

        // 4. Redis 无 Key：同步回源
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            stringRedisTemplate.opsForValue().set(redisKey, NULL_VALUE, NULL_VALUE_TTL);
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }

        EventDetailVO vo = toDetailVO(event);
        redisSet(redisKey, vo);
        caffeineCache.put(eventId, vo);
        return vo;
    }

    /**
     * 异步重建：查询 MySQL 并重建 Redis + Caffeine；DB 无数据时写空值。
     * 使用独立锁获取，避免与入口锁互相等待。
     */
    private void rebuildCache(Long eventId) {
        RLock lock = redissonClient.getLock(RedisKeys.lockEventRebuild(eventId));
        String redisKey = RedisKeys.eventDetail(eventId);
        try {
            if (!lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                log.info("[CACHE] rebuild lock busy eventId={}", eventId);
                return;
            }
            try {
                Event event = eventMapper.selectById(eventId);
                if (event == null) {
                    stringRedisTemplate.opsForValue().set(redisKey, NULL_VALUE, NULL_VALUE_TTL);
                    log.info("[CACHE] rebuild wrote null value eventId={}", eventId);
                    return;
                }
                EventDetailVO vo = toDetailVO(event);
                redisSet(redisKey, vo);
                log.info("[CACHE] rebuild refreshed eventId={}", eventId);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[CACHE] rebuild interrupted eventId={}", eventId);
        } catch (Exception e) {
            log.error("[CACHE] rebuild failed eventId={}", eventId, e);
        }
    }

    private void redisSet(String redisKey, EventDetailVO vo) {
        LocalDateTime logicalExpireAt = LocalDateTime.now().plus(LOGICAL_TTL);
        EventCachePayload payload = new EventCachePayload(vo, logicalExpireAt);
        String json = serialize(payload);
        // 物理 TTL：24 小时 + 0~30 分钟随机
        long physicalTtlSeconds = Duration.ofHours(24).toSeconds()
                + ThreadLocalRandom.current().nextLong(Duration.ofMinutes(30).toSeconds() + 1);
        stringRedisTemplate.opsForValue().set(redisKey, json, Duration.ofSeconds(physicalTtlSeconds));
    }

    private EventCachePayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, EventCachePayload.class);
        } catch (Exception e) {
            log.warn("[CACHE] deserialize failed, treat as missing: {}", e.getMessage());
            return null;
        }
    }

    private String serialize(EventCachePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "event cache serialize failed");
        }
    }

    private EventDetailVO toDetailVO(Event event) {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setArtist(event.getArtist());
        vo.setCategory(event.getCategory());
        vo.setDescription(event.getDescription());
        vo.setCityCode(event.getCityCode());
        vo.setVenueName(event.getVenueName());
        vo.setAddress(event.getAddress());
        vo.setCoverUrl(event.getCoverUrl());
        vo.setStartTime(event.getStartTime());
        vo.setEndTime(event.getEndTime());
        vo.setSaleStartTime(event.getSaleStartTime());
        vo.setSaleEndTime(event.getSaleEndTime());
        vo.setStatus(event.getStatus());
        vo.setHeatScore(event.getHeatScore());

        List<TicketSku> skus = ticketSkuMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<TicketSku>lambdaQuery()
                        .eq(TicketSku::getEventId, event.getId())
                        .eq(TicketSku::getStatus, 1));
        BigDecimal minPrice = skus.stream()
                .map(TicketSku::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        vo.setMinPrice(minPrice);
        return vo;
    }
}

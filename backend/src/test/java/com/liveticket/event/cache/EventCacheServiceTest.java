package com.liveticket.event.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.config.CaffeineConfig;
import com.liveticket.event.entity.Event;
import com.liveticket.event.mapper.EventMapper;
import com.liveticket.event.vo.EventDetailVO;
import com.liveticket.ticket.mapper.TicketSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventCacheServiceTest {

    private StringRedisTemplate stringRedisTemplate;
    private ValueOperations<String, String> valueOperations;
    private EventMapper eventMapper;
    private RLock rebuildLock;
    private ObjectMapper objectMapper;
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeine;
    private EventCacheService eventCacheService;

    private static final Long EVENT_ID = 1L;
    private static final String REDIS_KEY = "lt:event:detail:" + EVENT_ID;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stringRedisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        eventMapper = mock(EventMapper.class);

        TicketSkuMapper ticketSkuMapper = mock(TicketSkuMapper.class);
        when(ticketSkuMapper.selectList(any())).thenReturn(java.util.List.of());

        RedissonClient redissonClient = mock(RedissonClient.class);
        rebuildLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(rebuildLock);

        caffeine = Caffeine.newBuilder().maximumSize(10000)
                .expireAfterWrite(Duration.ofSeconds(60)).build();
        CaffeineCache caffeineCache = new CaffeineCache(CaffeineConfig.CACHE_EVENT_DETAIL, caffeine);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(CaffeineConfig.CACHE_EVENT_DETAIL)).thenReturn(caffeineCache);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ThreadPoolExecutor directExecutor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100)) {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        eventCacheService = new EventCacheService(
                stringRedisTemplate, eventMapper, ticketSkuMapper, redissonClient, cacheManager,
                directExecutor, objectMapper);
    }

    private Event sampleEvent() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Neon City Live");
        event.setArtist("Nova Echo");
        event.setCategory("concert");
        event.setDescription("desc");
        event.setCityCode("hangzhou");
        event.setVenueName("Venue");
        event.setAddress("Address");
        event.setCoverUrl("/covers/event-01.svg");
        event.setStatus(1);
        event.setHeatScore(9800L);
        return event;
    }

    private String payloadJson(EventDetailVO vo, LocalDateTime logicalExpireAt) throws Exception {
        return objectMapper.writeValueAsString(new EventCachePayload(vo, logicalExpireAt));
    }

    private EventDetailVO detailFrom(Event event) {
        EventDetailVO vo = new EventDetailVO();
        vo.setId(event.getId());
        vo.setTitle(event.getTitle());
        vo.setArtist(event.getArtist());
        vo.setStatus(event.getStatus());
        vo.setHeatScore(event.getHeatScore());
        return vo;
    }

    @Test
    void caffeineHitSkipsRedisAndDb() {
        EventDetailVO vo = detailFrom(sampleEvent());
        caffeine.put(EVENT_ID, vo);

        EventDetailVO result = eventCacheService.getEventDetail(EVENT_ID);

        assertThat(result.getId()).isEqualTo(EVENT_ID);
        verify(stringRedisTemplate, never()).opsForValue();
        verify(eventMapper, never()).selectById(any());
    }

    @Test
    void redisFreshHitWritesCaffeine() throws Exception {
        EventDetailVO vo = detailFrom(sampleEvent());
        when(valueOperations.get(REDIS_KEY))
                .thenReturn(payloadJson(vo, LocalDateTime.now().plusMinutes(10)));

        EventDetailVO result = eventCacheService.getEventDetail(EVENT_ID);

        assertThat(result.getTitle()).isEqualTo("Neon City Live");
        assertThat(caffeine.getIfPresent(EVENT_ID)).isEqualTo(vo);
        verify(eventMapper, never()).selectById(any());
    }

    @Test
    void nullValueCacheThrowsNotFound() {
        when(valueOperations.get(REDIS_KEY)).thenReturn(EventCacheService.NULL_VALUE);

        assertThatThrownBy(() -> eventCacheService.getEventDetail(EVENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode());
    }

    @Test
    void dbFallbackWritesRedisAndCaffeine() {
        Event event = sampleEvent();
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        when(eventMapper.selectById(EVENT_ID)).thenReturn(event);

        EventDetailVO result = eventCacheService.getEventDetail(EVENT_ID);

        assertThat(result.getId()).isEqualTo(EVENT_ID);
        assertThat(caffeine.getIfPresent(EVENT_ID)).isNotNull();
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(REDIS_KEY), anyString(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isGreaterThanOrEqualTo(Duration.ofHours(24));
    }

    @Test
    void dbMissingWritesNullValueAndThrows() {
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        when(eventMapper.selectById(EVENT_ID)).thenReturn(null);

        assertThatThrownBy(() -> eventCacheService.getEventDetail(EVENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode());
        verify(valueOperations).set(eq(REDIS_KEY), eq(EventCacheService.NULL_VALUE), eq(Duration.ofSeconds(120)));
    }

    @Test
    void logicalExpiredReturnsStaleValueAndRebuilds() throws Exception {
        Event staleEvent = sampleEvent();
        EventDetailVO staleVo = detailFrom(staleEvent);
        when(valueOperations.get(REDIS_KEY))
                .thenReturn(payloadJson(staleVo, LocalDateTime.now().minusMinutes(1)));

        Event dbEvent = sampleEvent();
        dbEvent.setTitle("Neon City Live v2");
        when(eventMapper.selectById(EVENT_ID)).thenReturn(dbEvent);
        when(rebuildLock.tryLock(Mockito.anyLong(), Mockito.anyLong(), any(TimeUnit.class))).thenReturn(true);

        EventDetailVO result = eventCacheService.getEventDetail(EVENT_ID);

        // 当前请求仍返回 Redis 旧数据
        assertThat(result.getTitle()).isEqualTo("Neon City Live");
        // 直接执行器同步完成重建：Redis 已被刷新
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(REDIS_KEY), jsonCaptor.capture(), any(Duration.class));
        EventCachePayload rebuilt = objectMapper.readValue(jsonCaptor.getValue(), EventCachePayload.class);
        assertThat(rebuilt.getData().getTitle()).isEqualTo("Neon City Live v2");
        assertThat(rebuilt.getLogicalExpireAt()).isAfter(LocalDateTime.now());
        // 入口锁 1 次 + 重建锁 1 次
        verify(rebuildLock, Mockito.times(2)).unlock();
    }

    @Test
    void logicalExpiredLockBusyStillReturnsStaleValue() throws Exception {
        EventDetailVO staleVo = detailFrom(sampleEvent());
        when(valueOperations.get(REDIS_KEY))
                .thenReturn(payloadJson(staleVo, LocalDateTime.now().minusMinutes(1)));
        when(rebuildLock.tryLock(Mockito.anyLong(), Mockito.anyLong(), any(TimeUnit.class))).thenReturn(false);

        EventDetailVO result = eventCacheService.getEventDetail(EVENT_ID);

        assertThat(result.getId()).isEqualTo(EVENT_ID);
        verify(eventMapper, never()).selectById(any());
        verify(rebuildLock, never()).unlock();
    }
}

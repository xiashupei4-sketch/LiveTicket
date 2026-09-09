package com.liveticket.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class CaffeineConfig {

    public static final String CACHE_EVENT_DETAIL = "eventDetail";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_EVENT_DETAIL);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofSeconds(60)));
        return cacheManager;
    }

    /**
     * 缓存逻辑过期异步重建线程池（规格第 12 节固定参数）
     */
    @Bean(name = "eventCacheRebuildExecutor")
    public ThreadPoolExecutor eventCacheRebuildExecutor() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "event-cache-rebuild-" + seq.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        return new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}


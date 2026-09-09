package com.liveticket.common.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Health", description = "健康检查")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Operation(summary = "健康检查", description = "无需 JWT")
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("application", "UP");
        status.put("mysql", checkMysql());
        status.put("redis", checkRedis());
        status.put("rabbitmq", checkRabbitmq());
        return ApiResponse.ok(status);
    }

    private String checkMysql() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            log.warn("[HEALTH] mysql DOWN: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            String pong = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("[HEALTH] redis DOWN: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkRabbitmq() {
        try {
            rabbitTemplate.execute(channel -> null);
            return "UP";
        } catch (Exception e) {
            log.warn("[HEALTH] rabbitmq DOWN: {}", e.getMessage());
            return "DOWN";
        }
    }
}

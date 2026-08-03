package com.asad.expensetracker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Shared-store rate limiter for multi-instance deployments: every backend instance increments
 * the same Redis counter, so the "20 requests per minute" limit applies platform-wide for a
 * given client, not per-process. Enable with app.security.rate-limit.backend=redis and point
 * REDIS_HOST/REDIS_PORT at a real Redis instance (docker-compose already wires this up).
 */
@Component
@ConditionalOnProperty(prefix = "app.security.rate-limit", name = "backend", havingValue = "redis")
public class RedisRateLimiter implements RateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final StringRedisTemplate redisTemplate;
    private final int maxRequestsPerWindow;

    @Value("${spring.application.name:expensetracker}")
    private String appName;

    public RedisRateLimiter(StringRedisTemplate redisTemplate,
                             @Value("${app.security.rate-limit.max-per-minute:20}") int maxRequestsPerWindow) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    @Override
    public boolean tryConsume(String key) {
        String redisKey = "ratelimit:" + appName + ":" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redisTemplate.expire(redisKey, WINDOW);
        }
        return count != null && count <= maxRequestsPerWindow;
    }
}

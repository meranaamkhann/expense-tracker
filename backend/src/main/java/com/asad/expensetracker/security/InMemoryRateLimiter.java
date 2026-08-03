package com.asad.expensetracker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default rate limiter: a fixed-window counter per key, held in process memory. Fine for a
 * single instance. If you scale the backend horizontally, switch to the Redis-backed one
 * (app.security.rate-limit.backend=redis) so all instances share the same window.
 */
@Component
@ConditionalOnProperty(prefix = "app.security.rate-limit", name = "backend", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private static final long WINDOW_MILLIS = 60_000; // 1 minute

    private final int maxRequestsPerWindow;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(@Value("${app.security.rate-limit.max-per-minute:20}") int maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    @Override
    public boolean tryConsume(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));

        synchronized (window) {
            if (now - window.startedAt > WINDOW_MILLIS) {
                window.startedAt = now;
                window.count.set(0);
            }
            return window.count.incrementAndGet() <= maxRequestsPerWindow;
        }
    }

    private static class Window {
        volatile long startedAt;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}

package com.asad.expensetracker.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    @Test
    void allowsUpToTheLimitThenBlocks() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(20);
        String key = "1.2.3.4";

        for (int i = 0; i < 20; i++) {
            assertThat(limiter.tryConsume(key)).as("request #%d should be allowed", i + 1).isTrue();
        }

        assertThat(limiter.tryConsume(key)).as("the 21st request in the same window should be blocked").isFalse();
    }

    @Test
    void tracksDifferentKeysIndependently() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(20);

        for (int i = 0; i < 20; i++) {
            limiter.tryConsume("client-a");
        }

        assertThat(limiter.tryConsume("client-a")).isFalse();
        assertThat(limiter.tryConsume("client-b")).isTrue();
    }
}

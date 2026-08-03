package com.asad.expensetracker.security;

/** Abstraction so the filter doesn't care whether counts live in-process or in a shared store. */
public interface RateLimiter {
    /** Returns true if this request is allowed, false if the caller has exceeded the limit. */
    boolean tryConsume(String key);
}

package com.lehnade.kobo.katas.ratelimiter.v1;

import java.util.function.LongSupplier;

public class TokenBucket {
    private final int capacity;
    private final int refillRate;
    private final LongSupplier clock;
    private long tokens;
    private long lastRefillTime;

    public TokenBucket(int capacity, int refillRate, LongSupplier clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefillTime = clock.getAsLong();
    }

    public boolean tryAcquire() {
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }
}

package com.lehnade.kobo.katas.ratelimiter.v2;

import java.util.function.LongSupplier;

public class TokenBucketV2 {
    private final int capacity;
    private final int refillRate;
    private final LongSupplier clock;
    private long lastRefillTime;
    private double tokens;

    public TokenBucketV2(int capacity, int refillRate, LongSupplier clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.tokens = capacity;
    }


    public boolean tryAcquire() {
        refill();
        if (tokens >= 1) {
            tokens--;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = clock.getAsLong();
        double tokensToAdd = (now - lastRefillTime) * refillRate / 1e9;
        tokens = Math.min(capacity, tokensToAdd);
        lastRefillTime = now;
    }
}

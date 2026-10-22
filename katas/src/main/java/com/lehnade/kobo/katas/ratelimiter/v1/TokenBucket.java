package com.lehnade.kobo.katas.ratelimiter.v1;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class TokenBucket {
    private final int capacity;
    private final int refillRate;
    private final LongSupplier clock;
    private double tokens;
    private long lastRefillTime;
    private ConcurrentHashMap<String, TokenBucket> buckets;

    public TokenBucket(int capacity, int refillRate, LongSupplier clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefillTime = clock.getAsLong();
        this.buckets = new ConcurrentHashMap<>();
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
        long currentTime = clock.getAsLong();
        double tokensToRefill = refillRate * (currentTime - lastRefillTime) / 1e9;
        tokens = Math.min(capacity, tokens + tokensToRefill);
        lastRefillTime = currentTime;
    }

    public boolean tryAcquire(String clientId) {
        buckets.computeIfAbsent(clientId, k -> new TokenBucket(capacity, refillRate, clock));

        return buckets.computeIfPresent(clientId, (k,v) -> v ).tryAcquire();
    }
}

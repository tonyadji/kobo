package com.lehnade.kobo.katas.ratelimiter.v1;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class RateLimiter {
    private final int capacity;
    private final int refillRate;
    private final LongSupplier clock;
    private ConcurrentHashMap<String, TokenBucket> buckets;

    public RateLimiter(int capacity, int refillRate, LongSupplier clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.buckets = new ConcurrentHashMap<>();
    }

    public boolean tryAcquire(String clientId) {
        return buckets.computeIfAbsent(clientId, k -> new TokenBucket(capacity, refillRate, clock))
                .tryAcquire();
    }
}

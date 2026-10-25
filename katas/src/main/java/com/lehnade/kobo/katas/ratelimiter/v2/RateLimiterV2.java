package com.lehnade.kobo.katas.ratelimiter.v2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class RateLimiterV2 {
    private final int capacity;
    private final int refillRate;
    private final LongSupplier clock;

    private final ConcurrentHashMap<String, TokenBucketV2> buckets;

    public RateLimiterV2(int capacity, int refillRate, LongSupplier clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.clock = clock;
        this.buckets = new ConcurrentHashMap<>();
    }

    public boolean tryAcquire(String clientId) {
        return buckets.computeIfAbsent(clientId, k -> new TokenBucketV2(capacity, refillRate, clock))
                .tryAcquire();
    }

}

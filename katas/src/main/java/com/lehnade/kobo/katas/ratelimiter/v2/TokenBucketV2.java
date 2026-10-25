package com.lehnade.kobo.katas.ratelimiter.v2;

public class TokenBucketV2 {
    private final int capacity;
    private final int refillRate;
    private int tokens;

    public TokenBucketV2(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
    }


    public boolean tryAcquire() {
        if (tokens >= 1) {
            tokens--;
            return true;
        }
        return false;
    }
}

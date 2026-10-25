package com.lehnade.kobo.katas.ratelimiter.v2;

public class TokenBucketV2 {

    private int tokens;

    public TokenBucketV2() {
        this.tokens = 1;
    }


    public boolean tryAcquire() {
        if (tokens >= 1) {
            tokens--;
            return true;
        }
        return false;
    }
}

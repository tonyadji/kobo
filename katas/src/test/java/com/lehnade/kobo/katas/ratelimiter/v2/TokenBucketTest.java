package com.lehnade.kobo.katas.ratelimiter.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenBucketTest {

    @Test
    void tryAcquireReturnsFalseIfRequestNotAllowed() {
        // arrange
        var tokenBucket = new TokenBucketV2();
        tokenBucket.tryAcquire();

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertFalse(result);
    }

    @Test
    void tryAcquireReturnsTrueIfRequestAllowed() {
        // arrange
        var tokenBucket = new TokenBucketV2();

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertTrue(result);
    }
}

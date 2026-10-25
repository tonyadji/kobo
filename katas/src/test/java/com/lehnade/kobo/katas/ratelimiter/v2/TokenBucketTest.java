package com.lehnade.kobo.katas.ratelimiter.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenBucketTest {

    @Test
    void tryAcquireReturnsFalseIfRequestNotAllowed() {
        // arrange
        var tokenBucket = new TokenBucketV2(10, 5);
        tokenBucket.tryAcquire();

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertFalse(result);
    }

    @Test
    void tryAcquireReturnsTrueIfRequestAllowed() {
        // arrange
        var tokenBucket = new TokenBucketV2(10, 5);

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertTrue(result);
    }

    @Test
    void theBucketStartsFull() {
        // arrange
        var tokenBucket = new TokenBucketV2(10, 5);
        var consumedTokens = 0;
        for (int i = 0; i < 10; i++) {
            if (tokenBucket.tryAcquire()) consumedTokens++; else break;
        }

        // act
        boolean requestNotAllowed = tokenBucket.tryAcquire();

        // assert
        Assertions.assertEquals(10, consumedTokens);
        Assertions.assertFalse(requestNotAllowed);
    }
}

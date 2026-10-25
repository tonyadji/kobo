package com.lehnade.kobo.katas.ratelimiter.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.LongSupplier;

public class TokenBucketTest {

    @Test
    void tryAcquireReturnsFalseIfRequestNotAllowed() {
        // arrange
        var tokenBucket = new TokenBucketV2(1, 5, new FakeClock());
        tokenBucket.tryAcquire();

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertFalse(result);
    }

    @Test
    void tryAcquireReturnsTrueIfRequestAllowed() {
        // arrange
        var tokenBucket = new TokenBucketV2(10, 5, new FakeClock());

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertTrue(result);
    }

    @Test
    void theBucketStartsFull() {
        // arrange
        var tokenBucket = new TokenBucketV2(10, 5, new FakeClock());
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

    @Test
    void shouldRefillTenTokensAfterTwoSeconds() {
        // arrange
        var clock = new FakeClock();
        var tokenBucket = new TokenBucketV2(10, 5, clock);
        for (int i = 0; i < 10; i++) {
            tokenBucket.tryAcquire();
        }
        var consumedTokens = 0;
        clock.advanceTime(Duration.ofSeconds(2));

        // act
        for (int i = 0; i < 10; i++) {
            if (tokenBucket.tryAcquire()) consumedTokens++; else break;
        }

        // assert
        Assertions.assertEquals(10, consumedTokens);
    }

    @Test
    void shouldNeverExceedsCapacityEvenAfterLongIdle() {
        // arrange
        var clock = new FakeClock();
        var tokenBucket = new TokenBucketV2(10, 5, clock);
        for (int i = 0; i < 10; i++) {
            tokenBucket.tryAcquire();
        }
        var consumedTokens = 0;
        clock.advanceTime(Duration.ofHours(1));

        // act
        for (int i = 0; i < 10; i++) {
            if (tokenBucket.tryAcquire()) consumedTokens++; else break;
        }
        boolean requestNotAllowed = tokenBucket.tryAcquire();

        // assert
        Assertions.assertFalse(requestNotAllowed);
        Assertions.assertEquals(10, consumedTokens);
    }

    @Test
    void emptyBucketShouldRefill1TokenAfter200Ms() {
        // arrange
        var clock = new FakeClock();
        var tokenBucket = new TokenBucketV2(10, 5, clock);
        for (int i = 0; i < 10; i++) {
            tokenBucket.tryAcquire();
        }
        clock.advanceTime(Duration.ofMillis(200));

        // act
        var clientAllowed = tokenBucket.tryAcquire();
        var clientNotAllowed = tokenBucket.tryAcquire();

        // assert
        Assertions.assertTrue(clientAllowed);
        Assertions.assertFalse(clientNotAllowed);
    }

    @Test
    void bucketWithTwoTokensShouldRefill5TokensAfter1s() {
        // arrange
        var clock = new FakeClock();
        var tokenBucket = new TokenBucketV2(10, 5, clock);
        for (int i = 0; i < 8; i++) {
            tokenBucket.tryAcquire();
        }
        clock.advanceTime(Duration.ofMillis(1000));
        var consumedTokens = 0;
        // act
        for (int i = 0; i < 10; i++) {
            if (tokenBucket.tryAcquire()) consumedTokens++; else break;
        }
        var clientNotAllowed = tokenBucket.tryAcquire();

        // assert
        Assertions.assertEquals(7, consumedTokens);
        Assertions.assertFalse(clientNotAllowed);
    }

    static class FakeClock implements LongSupplier {
        private long timeNanos;

        public FakeClock() {
            this.timeNanos = 0;
        }

        public void advanceTime(Duration duration) {
            this.timeNanos += duration.toNanos();
        }
        @Override
        public long getAsLong() {
            return timeNanos;
        }
    }
}

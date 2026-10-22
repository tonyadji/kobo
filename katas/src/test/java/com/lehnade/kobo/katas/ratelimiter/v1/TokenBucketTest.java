package com.lehnade.kobo.katas.ratelimiter.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.LongSupplier;

public class TokenBucketTest {

    @Test
    void tryAcquireShouldReturnTrueIfTokensAreAvailable() {
        // arrange
        var tokenBucket = new TokenBucket(10, 1, new FakeClock());

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertTrue(result);
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

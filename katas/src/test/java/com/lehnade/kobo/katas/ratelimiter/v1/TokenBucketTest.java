package com.lehnade.kobo.katas.ratelimiter.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void tryAcquireShouldReturnFalseIfBucketIsEmpty() {
        // arrange
        var tokenBucket = new TokenBucket(10, 1, new FakeClock());
        for (int i = 0; i < 10; i++) tokenBucket.tryAcquire();

        // act
        var result = tokenBucket.tryAcquire();

        // assert
        Assertions.assertFalse(result);
    }

    @Test
    // rate is 5/s so we need 2 seconds to fill the bucket
    void emptyBucketShouldRefillToCapacityAfterIdle() {
        // arrange
        FakeClock clock = new FakeClock();
        var tokenBucket = new TokenBucket(10, 5, clock);
        for (int i = 0; i < 10; i++) tokenBucket.tryAcquire();

        // act
        boolean tryWhenTokenIsEmpty = tokenBucket.tryAcquire();
        clock.advanceTime(Duration.ofSeconds(2));
        int accepted = 0;
        for (int i = 0; i < 10; i++) if(tokenBucket.tryAcquire()) accepted++;

        // assert
        Assertions.assertFalse(tryWhenTokenIsEmpty);
        Assertions.assertEquals(10, accepted);
    }

    @Test
    // rate is 5/s so we need 200 ms to fill one token
    void emptyBucketShouldRefillOneTokenAfter200ms () {
        // arrange
        FakeClock clock = new FakeClock();
        var tokenBucket = new TokenBucket(10, 5, clock);
        for (int i = 0; i < 10; i++) tokenBucket.tryAcquire();

        // act
        boolean tryWhenTokenIsEmpty = tokenBucket.tryAcquire();
        clock.advanceTime(Duration.ofMillis(200));
        int accepted = 0;
        for (int i = 0; i < 10; i++) if(tokenBucket.tryAcquire()) accepted++;

        // assert
        Assertions.assertFalse(tryWhenTokenIsEmpty);
        Assertions.assertEquals(1, accepted);
    }

    @Test
    // tokens never exceed capacity
    void refillShouldNeverExceedCapacityEvenAfterLongIdle() {
        // arrange
        FakeClock clock = new FakeClock();
        var tokenBucket = new TokenBucket(10, 5, clock);
        for (int i = 0; i < 10; i++) tokenBucket.tryAcquire();

        // act
        boolean tryWhenTokenIsEmpty = tokenBucket.tryAcquire();
        clock.advanceTime(Duration.ofHours(1));
        int accepted = 0;
        for (int i = 0; i < 10; i++) if(tokenBucket.tryAcquire()) accepted++;

        // assert
        Assertions.assertFalse(tryWhenTokenIsEmpty);
        Assertions.assertEquals(10, accepted);
    }

    @RepeatedTest(20)
    void thousandThreadsCallTryAcquireOnBucketWithCapacityOf100 () throws InterruptedException {
        int numberOfThreads = 1000;
        var tokenBucket = new TokenBucket(100, 1, new FakeClock());
        var threads = new Thread[numberOfThreads];
        var latch = new CountDownLatch(1);
        var accepted = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    latch.await();
                    if (tokenBucket.tryAcquire()) accepted.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i] = thread;
        }

        for (Thread thread : threads) thread.start();

        latch.countDown();
        for (Thread thread : threads) thread.join();

        Assertions.assertEquals(100, accepted.get());
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

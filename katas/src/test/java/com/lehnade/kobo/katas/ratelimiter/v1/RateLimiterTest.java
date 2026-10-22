package com.lehnade.kobo.katas.ratelimiter.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RateLimiterTest {

    @Test
    void tryAcquireShouldReturnTrueForANewClient() {
        // arrange
        String clientId = "client-1";
        var limiter = new RateLimiter(10, 1, new TokenBucketTest.FakeClock());

        // act
        var result = limiter.tryAcquire(clientId);

        // assert
        Assertions.assertTrue(result);
    }

    @Test
    void tryAcquireShouldReturnTrueForClientTwoAndFalseForClientOne() {
        // arrange
        String clientOne = "client-1";
        String clientTwo = "client-2";
        var limiter = new RateLimiter(10, 1, new TokenBucketTest.FakeClock());
        for (int i = 0; i < 10; i++) limiter.tryAcquire(clientOne);

        // act
        var resultOne = limiter.tryAcquire(clientOne);
        var resultTwo = limiter.tryAcquire(clientTwo);

        // assert
        Assertions.assertFalse(resultOne);
        Assertions.assertTrue(resultTwo);
    }
}

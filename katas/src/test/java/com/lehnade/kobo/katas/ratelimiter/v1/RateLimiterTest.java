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
}

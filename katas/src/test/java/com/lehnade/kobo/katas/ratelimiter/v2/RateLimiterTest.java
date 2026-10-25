package com.lehnade.kobo.katas.ratelimiter.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RateLimiterTest {

    @Test
    void eachClientHasItsOwnBucket() {
        // arrange
        String clientId = "client-1";
        String clientId2 = "client-2";
        var limiter = new RateLimiterV2(1, 5, new TokenBucketTest.FakeClock());

        // act
        var client1Allowed = limiter.tryAcquire(clientId);
        var client1NotAllowed = limiter.tryAcquire(clientId);
        var client2Allowed = limiter.tryAcquire(clientId2);

        // assert
        Assertions.assertTrue(client1Allowed);
        Assertions.assertTrue(client2Allowed);
        Assertions.assertFalse(client1NotAllowed);

    }
}

package com.lehnade.kobo.katas.loadbalancer.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoadBalancerTest {

    @Test
    void registerShouldRegisterInstance() {
        // arrange
        String address = "localhost:8080";
        var loadBalancer = new LoadBalancerV2();

        // act
        loadBalancer.register(address);

        // assert
        Assertions.assertTrue(loadBalancer.getInstancesList().contains(address));
        Assertions.assertEquals(1, loadBalancer.getInstancesList().size());
    }
}

package com.lehnade.kobo.katas.loadbalancer.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoadBalancerTest {

    @Test
    void shouldRegisterAndInstance() {
        // arrange
        String address = "localhost:8080";

        // act
        // the call to register method
        var loadBalancer = new LoadBalancer();
        loadBalancer.register(address);

        // assert
        // -> the new instance should be registered in some data structure
        Assertions.assertTrue(loadBalancer.getInstances().contains(address));
    }
}

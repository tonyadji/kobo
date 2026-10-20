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

    @Test
    void atMostTenInstancesCanBeRegistered() {
        // arrange
        var loadBalancer = new LoadBalancer();
        for (int i = 0; i < 10; i++) {
            loadBalancer.register("localhost:800" + i);
        }

        // act
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.register("localhost:80010"));

        // assert
        Assertions.assertEquals("Maximum number of instances reached", exception.getMessage());
        Assertions.assertEquals(10, loadBalancer.getInstances().size());
    }

    @Test
    void theSameAddressCannotBeRegisteredTwice() {
        // arrange
        String address = "localhost:8080";
        var loadBalancer = new LoadBalancer();
        loadBalancer.register(address);

        // act
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.register(address));

        // assert
        Assertions.assertEquals("Instance already registered", exception.getMessage());
        Assertions.assertEquals(1, loadBalancer.getInstances().size());
    }

    @Test
    void nullOrBlankMustBeRejected() {
        // arrange
        String blankAddress = "";
        var loadBalancer = new LoadBalancer();

        // act
        IllegalArgumentException nullAddressException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loadBalancer.register(null));

        IllegalArgumentException blankAddressException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loadBalancer.register(blankAddress));

        // assert
        Assertions.assertEquals("Address cannot be null or blank", nullAddressException.getMessage());
        Assertions.assertEquals("Address cannot be null or blank", blankAddressException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstances().size());
    }
}

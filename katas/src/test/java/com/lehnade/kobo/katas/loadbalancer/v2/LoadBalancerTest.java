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

    @Test
    void atMost10InstancesCanBeRegistered() {
        // arrange
        var loadBalancer = new LoadBalancerV2();
        for (int i = 0; i < 10; i++) {
            loadBalancer.register("localhost:" + (8000+i));
        }

        // act & assert
        IllegalStateException tooMuchInstances = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.register("localhost:8091"));

        Assertions.assertEquals("Maximum number of instances reached", tooMuchInstances.getMessage());
        Assertions.assertEquals(10, loadBalancer.getInstancesList().size());
    }

    @Test
    void theSameAddressCannotBeRegisteredTwice() {
        // arrange
        String address = "localhost:8080";
        var loadBalancer = new LoadBalancerV2();
        loadBalancer.register(address);

        // act
        IllegalStateException alreadyRegistered = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.register(address));

        Assertions.assertEquals("Instance already registered", alreadyRegistered.getMessage());
        Assertions.assertEquals(1, loadBalancer.getInstancesList().size());
    }

    @Test
    void nullOrBlankAddressMustBeRejected() {
        // arrange
        String blankAddress = "";
        String nullAddress = null;
        var loadBalancer = new LoadBalancerV2();

        // act & assert
        IllegalArgumentException blankAddressException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loadBalancer.register(blankAddress));
        IllegalArgumentException nullAddressException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loadBalancer.register(nullAddress));

        Assertions.assertEquals("Address cannot be null or blank", blankAddressException.getMessage());
        Assertions.assertEquals("Address cannot be null or blank", nullAddressException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstancesList().size());
    }
}

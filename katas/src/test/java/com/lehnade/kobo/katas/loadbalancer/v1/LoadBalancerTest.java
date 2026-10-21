package com.lehnade.kobo.katas.loadbalancer.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoadBalancerTest {

    LoadBalancerStrategy randomStrategy = new RandomStrategy();
    LoadBalancerStrategy roundRobinStrategy = new RoundRobinStrategy();

    @Test
    void shouldRegisterAndInstance() {
        // arrange
        String address = "localhost:8080";

        // act
        // the call to register method
        var loadBalancer = new LoadBalancer(randomStrategy);
        loadBalancer.register(address);

        // assert
        // -> the new instance should be registered in some data structure
        Assertions.assertTrue(loadBalancer.getInstances().contains(address));
    }

    @Test
    void atMostTenInstancesCanBeRegistered() {
        // arrange
        var loadBalancer = new LoadBalancer(randomStrategy);
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
        var loadBalancer = new LoadBalancer(randomStrategy);
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
        var loadBalancer = new LoadBalancer(randomStrategy);

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

    @Test
    void getShouldFailWithNoInstancesRegistered() {
        // arrange
        var loadBalancer = new LoadBalancer(randomStrategy);

        // act
        IllegalStateException noInstancesRegisteredException = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.get());

        // assert
        Assertions.assertEquals("No instances registered", noInstancesRegisteredException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstances().size());
    }

    @Test
    void getShouldReturnOneRegisteredInstanceAtRandom() {
        // arrange
        var loadBalancer = new LoadBalancer(randomStrategy);
        for (int i = 0; i < 10; i++) {
            loadBalancer.register("localhost:800" + i);
        }

        // act
        String instance = loadBalancer.get();

        // assert
        Assertions.assertNotNull(instance);
        Assertions.assertTrue(loadBalancer.getInstances().contains(instance));
    }

    @Test
    void shouldReturnConsecutiveInstancesWithRoundRobinStrategy() {
        // arrange
        var loadBalancer = new LoadBalancer(roundRobinStrategy);
        for (int i = 0; i < 3; i++) {
            loadBalancer.register("localhost:800" + i);
        }

        // act
        String instance1 = loadBalancer.get();
        String instance2 = loadBalancer.get();
        String instance3 = loadBalancer.get();
        String instance4 = loadBalancer.get();

        // assert
        Assertions.assertEquals("localhost:8000", instance1);
        Assertions.assertEquals("localhost:8001", instance2);
        Assertions.assertEquals("localhost:8002", instance3);
        Assertions.assertEquals("localhost:8000", instance4);
    }

    @Test
    void shouldUnregisterAddress() {
        // arrange
        var loadBalancer = new LoadBalancer(randomStrategy);
        for (int i = 0; i < 10; i++) {
            loadBalancer.register("localhost:800" + i);
        }
        String address = "localhost:8005";

        // act
        loadBalancer.unregister(address);

        // assert
        Assertions.assertFalse(loadBalancer.getInstances().contains(address));
        Assertions.assertEquals(9, loadBalancer.getInstances().size());
    }

    @Test
    void unregisterShouldFailWhenNoInstancesRegistered() {
        // arrange
        var loadBalancer = new LoadBalancer(randomStrategy);

        // act
        IllegalStateException noInstancesRegisteredException = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.unregister("some instance"));

        // assert
        Assertions.assertEquals("No instances registered", noInstancesRegisteredException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstances().size());
    }
}

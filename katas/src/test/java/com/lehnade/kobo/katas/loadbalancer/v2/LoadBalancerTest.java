package com.lehnade.kobo.katas.loadbalancer.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LoadBalancerTest {

    @Test
    void registerShouldRegisterInstance() {
        // arrange
        String address = "localhost:8080";
        var loadBalancer = new LoadBalancerV2(new FakeRandomStrategy());

        // act
        loadBalancer.register(address);

        // assert
        Assertions.assertTrue(loadBalancer.getInstancesList().contains(address));
        Assertions.assertEquals(1, loadBalancer.getInstancesList().size());
    }

    @Test
    void atMost10InstancesCanBeRegistered() {
        // arrange
        var loadBalancer = new LoadBalancerV2(new FakeRandomStrategy());
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
        var loadBalancer = new LoadBalancerV2(new FakeRandomStrategy());
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
        var loadBalancer = new LoadBalancerV2(new FakeRandomStrategy());

        // act & assert
        IllegalArgumentException blankAddressException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loadBalancer.register(blankAddress));
        IllegalArgumentException nullAddressException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> loadBalancer.register(nullAddress));

        Assertions.assertEquals("Address cannot be null or blank", blankAddressException.getMessage());
        Assertions.assertEquals("Address cannot be null or blank", nullAddressException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstancesList().size());
    }

    @Test
    void getFailsClearlyWithNoRegisteredInstances() {
        // arrange
        var loadBalancer = new LoadBalancerV2(new FakeRandomStrategy());

        // act
        IllegalStateException noInstancesRegisteredException = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.get());

        Assertions.assertEquals("No instances registered", noInstancesRegisteredException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstancesList().size());
    }
    
    @Test
    void getShouldReturnOneRegisteredInstanceAtRandom() {
        // arrange
        var randomStrategy = new FakeRandomStrategy();
        var loadBalancer = new LoadBalancerV2(randomStrategy);
        for (int i = 0; i < 10; i++) {
            loadBalancer.register("localhost:" + (8000+i));
        }
        
        // act
        randomStrategy.setIndex(4);
        String the5thInstance = loadBalancer.get();

        randomStrategy.setIndex(1);
        String the2ndInstance = loadBalancer.get();

        randomStrategy.setIndex(9);
        String then10thInstance = loadBalancer.get();
        
        // assert
        Assertions.assertEquals("localhost:8004", the5thInstance);
        Assertions.assertEquals("localhost:8001", the2ndInstance);
        Assertions.assertEquals("localhost:8009", then10thInstance);
    }
    
    static class FakeRandomStrategy implements LoadBalancerStrategy {
        private int index = 0;
        
        @Override
        public String get(List<String> instances) {
            return instances.get(index);
        }
        
        public void setIndex(int index) {
            this.index = index;
        }
    }
}

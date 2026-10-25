package com.lehnade.kobo.katas.loadbalancer.v2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class LoadBalancerTest {

    @Test
    void registerShouldRegisterInstance() {
        // arrange
        String address = "localhost:8080";
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());

        // act
        loadBalancer.register(address);

        // assert
        Assertions.assertTrue(loadBalancer.getInstancesList().contains(address));
        Assertions.assertEquals(1, loadBalancer.getInstancesList().size());
    }

    @Test
    void atMost10InstancesCanBeRegistered() {
        // arrange
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());
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
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());
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
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());

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
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());

        // act
        IllegalStateException noInstancesRegisteredException = Assertions.assertThrows(IllegalStateException.class,
                () -> loadBalancer.get());

        Assertions.assertEquals("No instances registered", noInstancesRegisteredException.getMessage());
        Assertions.assertEquals(0, loadBalancer.getInstancesList().size());
    }
    
    @Test
    void getShouldReturnOneRegisteredInstanceAtRandom() {
        // arrange
        final int[] nextValueToReturn = new int[1];
        IntUnaryOperator bound = (int i) -> nextValueToReturn[0];
        var randomStrategy = new RandomStrategy(bound);
        var loadBalancer = new LoadBalancerV2(randomStrategy);
        for (int i = 0; i < 10; i++) {
            loadBalancer.register("localhost:" + (8000+i));
        }
        
        // act
        nextValueToReturn[0] = 4;
        String the5thInstance = loadBalancer.get();

        nextValueToReturn[0] = 1;
        String the2ndInstance = loadBalancer.get();

        nextValueToReturn[0] = 9;
        String then10thInstance = loadBalancer.get();
        
        // assert
        Assertions.assertEquals("localhost:8004", the5thInstance);
        Assertions.assertEquals("localhost:8001", the2ndInstance);
        Assertions.assertEquals("localhost:8009", then10thInstance);
    }

    @Test
    void getShouldReturnAdjacentInstancesWithRoundRobinStrategy() {
        // arrange
        var loadBalancer = new LoadBalancerV2(new RoundRobinStrategy());
        for (int i = 0; i < 3; i++) {
            loadBalancer.register("localhost:" + (8000+i));
        }

        // act
        String the1stInstance = loadBalancer.get();
        String the2ndInstance = loadBalancer.get();
        String the3rdInstance = loadBalancer.get();
        String the4thInstance = loadBalancer.get();

        // assert
        Assertions.assertEquals("localhost:8000", the1stInstance);
        Assertions.assertEquals("localhost:8001", the2ndInstance);
        Assertions.assertEquals("localhost:8002", the3rdInstance);
        Assertions.assertEquals("localhost:8000", the4thInstance);
    }

    @Test
    void unregisterShouldUnregisterInstance() {
        // arrange
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());
        String address = "localhost:8080";
        String address2 = "localhost:8082";
        loadBalancer.register(address);
        loadBalancer.register(address2);

        // act
        boolean unregistered = loadBalancer.unregister(address);

        // assert
        Assertions.assertTrue(unregistered);
        Assertions.assertFalse(loadBalancer.getInstancesList().contains(address));
        Assertions.assertEquals(1, loadBalancer.getInstancesList().size());
    }

    @Test
    void unregisterShouldReturnFalseWhenCallWithNoInstances() {
        // arrange
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());

        // act
        boolean unregistered = loadBalancer.unregister("localhost:8080");

        // assert
        Assertions.assertFalse(unregistered);
    }

    @RepeatedTest(10)
    void thousandThreadShouldCallRegisterAndOnlyTenSucceed() throws InterruptedException {
        // arrange
        var loadBalancer = new LoadBalancerV2(new RandomStrategy());
        var threadCount = 1000;
        var threads = new Thread[threadCount];
        var success = new AtomicInteger(0);
        var counter = new AtomicInteger(0);
        var latch = new CountDownLatch(1);
        var errors = new ConcurrentLinkedQueue<String>();

        for (int i = 0; i < threadCount; i++) {

            threads[i] = new Thread(() -> {
                try {
                    latch.await();
                    loadBalancer.register("localhost:"+(8000+counter.incrementAndGet()));
                    success.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }
            });
        }

        // act
        for (Thread thread : threads) thread.start();
        latch.countDown();
        for (Thread thread : threads) thread.join();

        // assert
        Assertions.assertEquals(10, loadBalancer.getInstancesList().size());
        Assertions.assertEquals(10, success.get());
        Assertions.assertEquals(990, errors.size());
    }

}

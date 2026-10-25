package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntUnaryOperator;

public class RandomStrategy implements LoadBalancerStrategy {
    private final IntUnaryOperator nextInt;

    public RandomStrategy(IntUnaryOperator nextInt) {
        this.nextInt = nextInt;
    }

    public RandomStrategy() {
        this(bound -> ThreadLocalRandom.current().nextInt(bound));
    }

    @Override
    public String get(List<String> instances) {

        return instances.get(nextInt.applyAsInt(instances.size()));
    }
}

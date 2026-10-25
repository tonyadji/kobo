package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements LoadBalancerStrategy {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String get(List<String> instances) {
        return instances.get(Math.floorMod(counter.getAndIncrement(), instances.size()));
    }
}

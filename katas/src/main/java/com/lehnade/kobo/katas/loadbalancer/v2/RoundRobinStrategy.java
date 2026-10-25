package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.List;

public class RoundRobinStrategy implements LoadBalancerStrategy {
    private int counter = 0;

    @Override
    public String get(List<String> instances) {
        return instances.get(counter++ % instances.size());
    }
}

package com.lehnade.kobo.katas.loadbalancer.v1;

import java.util.List;
import java.util.Random;

public class RandomStrategy implements LoadBalancerStrategy {
    @Override
    public String get(List<String> instances) {
        int index = new Random().nextInt(instances.size());
        return instances.get(index);
    }
}

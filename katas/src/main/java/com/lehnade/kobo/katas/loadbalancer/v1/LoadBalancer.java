package com.lehnade.kobo.katas.loadbalancer.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class LoadBalancer {

    private final ArrayList<String> instances;
    private final LoadBalancerStrategy strategy;

    public LoadBalancer(LoadBalancerStrategy strategy) {
        this.strategy = strategy;
        instances = new ArrayList<>();
    }

    public void register(String address) {
        if (instances.size() >= 10) {
            throw new IllegalStateException("Maximum number of instances reached");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank");
        }
        if (instances.contains(address)) {
            throw new IllegalStateException("Instance already registered");
        }
        instances.add(address);
    }

    public Collection<String> getInstances() {
        return instances;
    }

    public String get() {
        if (instances.isEmpty()) {
            throw new IllegalStateException("No instances registered");
        }
        return strategy.get(instances);
    }
}

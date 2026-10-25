package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerV2 {

    private static final int MAXIMUM_INSTANCES = 10;
    private volatile List<String> instances;
    private final LoadBalancerStrategy strategy;

    public LoadBalancerV2(LoadBalancerStrategy strategy) {
        this.strategy = strategy;
        instances = new ArrayList<>();
    }

    public synchronized void register(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank");
        }
        if (instances.contains(address)) {
            throw new IllegalStateException("Instance already registered");
        }
        if (instances.size() >= MAXIMUM_INSTANCES) {
            throw new IllegalStateException("Maximum number of instances reached");
        }
        var copy = new ArrayList<>(instances);
        copy.add(address);
        instances = List.copyOf(copy);
    }

    public List<String> getInstancesList() {
        return List.copyOf(instances);
    }

    public String get() {
        var snapshot = instances;
        if (snapshot.isEmpty()) {
            throw new IllegalStateException("No instances registered");
        }
        return strategy.get(snapshot);
    }

    public synchronized boolean unregister(String address) {
        var copy = new ArrayList<>(instances);
        boolean result = copy.remove(address);
        instances = List.copyOf(copy);
        return result;
    }
}

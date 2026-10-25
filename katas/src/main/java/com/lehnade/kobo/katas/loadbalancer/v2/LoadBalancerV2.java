package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerV2 {

    private static final int MAXIMUM_INSTANCES = 10;
    private final List<String> instances;

    public LoadBalancerV2() {
        instances = new ArrayList<>();
    }

    public void register(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address cannot be null or blank");
        }
        if (instances.contains(address)) {
            throw new IllegalStateException("Instance already registered");
        }
        if (instances.size() >= MAXIMUM_INSTANCES) {
            throw new IllegalStateException("Maximum number of instances reached");
        }
        instances.add(address);
    }

    public List<String> getInstancesList() {
        return List.copyOf(instances);
    }
}

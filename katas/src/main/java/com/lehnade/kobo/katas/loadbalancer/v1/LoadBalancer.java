package com.lehnade.kobo.katas.loadbalancer.v1;

import java.util.ArrayList;
import java.util.Collection;

public class LoadBalancer {

    private final ArrayList<String> instances;

    public LoadBalancer() {
        instances = new ArrayList<>();
    }

    public void register(String address) {
        if (instances.size() >= 10) {
            throw new IllegalStateException("Maximum number of instances reached");
        }
        if (instances.contains(address)) {
            throw new IllegalStateException("Instance already registered");
        }
        instances.add(address);
    }

    public Collection<String> getInstances() {
        return instances;
    }
}

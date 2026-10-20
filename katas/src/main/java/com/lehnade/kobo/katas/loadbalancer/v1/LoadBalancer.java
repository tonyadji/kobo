package com.lehnade.kobo.katas.loadbalancer.v1;

import java.util.ArrayList;
import java.util.Collection;

public class LoadBalancer {

    private final ArrayList<String> instances;

    public LoadBalancer() {
        instances = new ArrayList<>();
    }

    public void register(String address) {
        instances.add(address);
    }

    public Collection<String> getInstances() {
        return instances;
    }
}

package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerV2 {

    private final List<String> instances;

    public LoadBalancerV2() {
        instances = new ArrayList<>();
    }

    public void register(String address) {
        instances.add(address);
    }

    public List<String> getInstancesList() {
        return List.copyOf(instances);
    }
}

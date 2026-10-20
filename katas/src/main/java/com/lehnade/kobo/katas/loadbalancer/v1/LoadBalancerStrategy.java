package com.lehnade.kobo.katas.loadbalancer.v1;

import java.util.List;

public interface LoadBalancerStrategy {

    String get(List<String> instances);
}

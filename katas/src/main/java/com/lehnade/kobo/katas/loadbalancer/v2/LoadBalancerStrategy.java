package com.lehnade.kobo.katas.loadbalancer.v2;

import java.util.List;

public interface LoadBalancerStrategy {

    String get(List<String> instances);
}

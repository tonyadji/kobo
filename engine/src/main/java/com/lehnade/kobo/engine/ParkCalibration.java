package com.lehnade.kobo.engine;

import java.util.concurrent.locks.LockSupport;

public final class ParkCalibration {
    private ParkCalibration() {}

    public static void main(String[] args) {
        long t = System.nanoTime();
        for (int i = 0; i < 10_000; i++) LockSupport.parkNanos(20_000);
        System.out.println((System.nanoTime() - t) / 10_000 / 1_000.0 + " µs per park");
    }

}

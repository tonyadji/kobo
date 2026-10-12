package com.lehnade.kobo.engine;

import java.util.concurrent.locks.LockSupport;

public class Work {
    static volatile long micros;

    static void simulate() {
        if (micros > 0) {
            LockSupport.parkNanos(micros * 1_000);
        }
    }
}

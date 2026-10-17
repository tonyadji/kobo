package com.lehnade.kobo.engine;

public interface TransferEngine {
    void transfer(int from, int to, long amount);

    long total();

    default long casFailures() { return 0L; }

    default void drain() throws InterruptedException {}
}
package com.lehnade.kobo.engine;

public interface TransferEngine {
    void transfer(int from, int to, long amount);

    long total();

    default void drain() throws InterruptedException {}
}
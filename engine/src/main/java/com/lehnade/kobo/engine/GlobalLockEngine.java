package com.lehnade.kobo.engine;

import java.util.Arrays;

public class GlobalLockEngine implements TransferEngine {
    private final Object lock = new Object();
    private final long[] balances;

    public GlobalLockEngine(int accounts, long initialBalance) {
        this.balances = new long[accounts];
        Arrays.fill(balances, initialBalance);
    }

    @Override
    public void transfer(int from, int to, long amount) {
        synchronized (lock) {
            if (balances[from] < amount) {
                throw new InsufficientFundsException();
            }
            balances[from] -= amount;
            balances[to] += amount;
            Work.simulate();
        }

    }

    @Override
    public long total() {
        synchronized (lock) {
            long sum = 0;
            for (long balance : balances) {
                sum += balance;
            }
            return sum;
        }
    }
}

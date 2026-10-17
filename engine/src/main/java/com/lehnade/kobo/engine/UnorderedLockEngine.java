package com.lehnade.kobo.engine;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class UnorderedLockEngine implements TransferEngine {
    private final long[] balances;
    private final ReentrantLock[] locks;

    public UnorderedLockEngine(int accounts, long initialBalance) {
        this.balances = new long[accounts];
        this.locks = new ReentrantLock[accounts];
        Arrays.fill(balances, initialBalance);
        Arrays.setAll(locks, i -> new ReentrantLock());
    }
    @Override
    public void transfer(int from, int to, long amount) {
        lock(from);
        lock(to);
        try {
            if (balances[from] < amount) {
                throw new InsufficientFundsException();
            }
            balances[from] -= amount;
            balances[to] += amount;
            Work.simulate();
        } finally {
            unlock(from);
            unlock(to);
        }
    }

    // not a consistent snapshot while transfers run
    @Override
    public long total() {
        long sum = 0;
        for (long balance : balances) {
            sum += balance;
        }
        return sum;
    }

    private void lock(int id) {
        locks[id].lock();
    }

    private void unlock(int id) {
        locks[id].unlock();
    }
}

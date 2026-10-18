package com.lehnade.kobo.engine;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

public class OrderedLockEngine implements TransferEngine {
    private final long[] balances;
    private final ReentrantLock[] locks;

    public OrderedLockEngine(int accounts, long initialBalance) {
        this.balances = new long[accounts];
        this.locks = new ReentrantLock[accounts];
        Arrays.fill(balances, initialBalance);
        Arrays.setAll(locks, i -> new ReentrantLock());
    }
    @Override
    public void transfer(int from, int to, long amount) {
        int first = Math.min(from, to);
        int second = Math.max(from, to);
        lock(first);
        lock(second);

        try {
            if (balances[from] < amount) {
                throw new InsufficientFundsException();
            }
            balances[from] -= amount;
            balances[to] += amount;
            Work.simulate();
        } finally {
            unlock(second);
            unlock(first);
        }

    }


    @Override
    public long total() {
        lockAll();
        try {
            long sum = 0;
            for (long balance : balances) {
                sum += balance;
            }
            return sum;
        } finally {
            unlockAll();
        }
    }

    private void lock(int id) {
        locks[id].lock();
    }

    private void unlock(int id) {
        locks[id].unlock();
    }

    private void lockAll() {
        for (int i = 0; i < locks.length; i++) {
            locks[i].lock();
        }
    }

    private void unlockAll() {
        for (int i = 0; i < locks.length; i++) {
            locks[i].unlock();
        }
    }
}

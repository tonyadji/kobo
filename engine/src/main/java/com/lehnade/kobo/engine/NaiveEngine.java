package com.lehnade.kobo.engine;

import java.util.Arrays;

public class NaiveEngine implements TransferEngine {
    private final long[] balances;

    public NaiveEngine(int accounts, long initialBalance) {
        balances = new long[accounts];
        Arrays.fill(balances, initialBalance);
    }

    @Override
    public void transfer(int from, int to, long amount) {
        if (balances[from] < amount) {
            throw new InsufficientFundsException();
        }
        balances[from] -= amount;
        balances[to] += amount;
        Work.simulate();
    }

    public long total() {
        long sum = 0;
        for (long balance : balances) {
            sum += balance;
        }
        return sum;
    }
}

package com.lehnade.kobo.engine;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

public class AtomicEngine implements TransferEngine {
    private static final LongAdder casFailures = new LongAdder();
    private final AtomicLongArray balances;

    public AtomicEngine(int accounts, long initialBalance) {
        long[] initialArray= new long[accounts];
        Arrays.fill(initialArray, initialBalance);
        this.balances = new AtomicLongArray(initialArray);

    }

    @Override
    public void transfer(int from, int to, long amount) {
        while (true) {
            long fromBalance = balances.get(from);
            if (fromBalance < amount) {
                throw new InsufficientFundsException();
            }
            if (!balances.compareAndSet(from, fromBalance, fromBalance - amount)) {
                casFailures.increment();
            } else {
                break;
            }
        }
        Work.simulate();
        balances.addAndGet(to, amount);
    }

    @Override
    public long total() {
        long sum = 0;
        for (int i = 0; i < balances.length(); i++) {
            sum += balances.get(i);
        }
        return sum;
    }

    @Override
    public long casFailures() {
        return casFailures.longValue();
    }
}

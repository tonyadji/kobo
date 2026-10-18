package com.lehnade.kobo.engine;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ShardedQueueEngine implements TransferEngine {
    private final long[] balances;
    private final ExecutorService[] shards;

    public ShardedQueueEngine(int accounts, long initialBalance) {
        balances = new long[accounts];
        Arrays.fill(balances, initialBalance);
        shards = new ExecutorService[Runtime.getRuntime().availableProcessors()];
        Arrays.setAll(shards, i-> Executors.newSingleThreadExecutor());
    }

    @Override
    public void transfer(int from, int to, long amount) {
        Future<?> debit = shardOf(from).submit(() -> {
            if (balances[from] < amount) {
                throw new InsufficientFundsException();
            }
            balances[from] -= amount;
            Work.simulate();
            shardOf(to).execute(() -> balances[to] += amount);
        });
        try {
            debit.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof InsufficientFundsException insufficient) {
                throw insufficient;
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private ExecutorService shardOf(int id) {
        return shards[id % shards.length];
    }

    @Override
    public long total() {
        long sum = 0;
        for (long balance : balances) {
            sum += balance;
        }
        return sum;
    }

    @Override
    public void drain() throws InterruptedException {
        for (ExecutorService shard : shards) {
            try {
                shard.submit(() -> {}).get();
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
            shard.shutdown();
        }
    }
}

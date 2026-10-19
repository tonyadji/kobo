package com.lehnade.kobo.engine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineInvariantTest {
    static final int ACCOUNTS = 10, THREADS = 200, TRANSFERS = 1_000;
    static final long INITIAL_BALANCE = 5_000;

    @ParameterizedTest
    @ValueSource(strings = {"globalLock","orderedLock","atomicEngine","queue"})
    void moneyIsNeverCreatedNorDestroyed(String name) throws Exception {
        TransferEngine engine = Engines.create(name, ACCOUNTS, INITIAL_BALANCE);
        long before = engine.total();
        var start = new CountDownLatch(1);

        try (var pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                pool.submit(() -> {
                    start.await();
                    var random = ThreadLocalRandom.current();
                    for (int i = 0; i < TRANSFERS; i++) {
                        int from = random.nextInt(ACCOUNTS), to = random.nextInt(ACCOUNTS);
                        if (from == to) continue;
                        try {
                            engine.transfer(from, to, 1 + random.nextInt(1_000));
                        } catch (InsufficientFundsException ignored) {
                        }
                    }
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "stuck: deadlock?");
        }
        engine.drain();
        assertEquals(before, engine.total());
    }
}
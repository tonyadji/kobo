package com.lehnade.kobo.engine;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

public class EngineLab {
    static final long INITIAL_BALANCE = 5_000;

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parse(args);
        String name = options.getOrDefault("engine", "naive");
        int accounts = Integer.parseInt(options.getOrDefault("accounts", "10"));
        int threads = Integer.parseInt(options.getOrDefault("threads", "1000"));
        int transfers = Integer.parseInt(options.getOrDefault("transfers", "1000"));
        int hot = Integer.parseInt(options.getOrDefault("hot", "0"));
        boolean virtual = options.containsKey("virtual");
        boolean audit = options.containsKey("audit");
        Work.micros = Long.parseLong(options.getOrDefault("work", "0"));

        TransferEngine engine = Engines.create(name, accounts, INITIAL_BALANCE);
        long before = engine.total();

        var ok = new LongAdder();
        var rejected = new LongAdder();
        var start = new CountDownLatch(1);
        ExecutorService pool = virtual
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await();
                var random = ThreadLocalRandom.current();
                for (int i = 0; i < transfers; i++) {
                    int from = random.nextInt(accounts);
                    int to = random.nextInt(accounts);
                    if (random.nextInt(100) < hot) {
                        if (random.nextBoolean()) from = 0; else to = 0;
                    }
                    if (from == to) continue;
                    try {
                        engine.transfer(from, to, 1 + random.nextInt(1_000));
                        ok.increment();
                    } catch (InsufficientFundsException e) {
                        rejected.increment();
                    }
                }
                return null;
            });
        }

        long auditMin = Long.MAX_VALUE, auditMax = Long.MIN_VALUE;
        long t0 = System.nanoTime();
        start.countDown();
        pool.shutdown();
        long deadline = t0 + TimeUnit.SECONDS.toNanos(30);
        while (!pool.awaitTermination(1, TimeUnit.MILLISECONDS)) {
            if (System.nanoTime() > deadline) {
                long[] deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
                System.out.printf("%s: STUCK after 30 s, ok=%d, deadlocked threads=%d%n",
                        name, ok.sum(), deadlocked == null ? 0 : deadlocked.length);
                System.exit(2);
            }
            if (audit) {
                long sum = engine.total();
                auditMin = Math.min(auditMin, sum);
                auditMax = Math.max(auditMax, sum);
            }
        }
        engine.drain();
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long after = engine.total();

        System.out.printf("%s accounts=%d threads=%d%s work=%dµs hot=%d%%: %d ms, ok=%d, rejected=%d, ok/s=%d%n",
                name, accounts, threads, virtual ? " (virtual)" : "", Work.micros, hot, ms,
                ok.sum(), rejected.sum(), ms == 0 ? 0 : ok.sum() * 1000 / ms);
        if (audit) {
            System.out.printf("audit during run: min=%d max=%d%n", auditMin, auditMax);
        }
        System.out.printf("invariant: before=%d after=%d (%+d) -> %s%n",
                before, after, after - before, before == after ? "OK" : "BROKEN");
    }

    static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            String[] kv = arg.replaceFirst("^--", "").split("=", 2);
            options.put(kv[0], kv.length > 1 ? kv[1] : "true");
        }
        return options;
    }
}

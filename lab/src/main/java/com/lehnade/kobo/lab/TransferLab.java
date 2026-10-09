package com.lehnade.kobo.lab;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class TransferLab {
    static final String URL = "jdbc:postgresql://localhost:5432/kobo";
    static final int WORKERS = 16, TRANSFERS_PER_WORKER = 50, ACCOUNTS = 10;
    static final long INITIAL_BALANCE = 100_000;


    public static void main(String[] args) throws SQLException, InterruptedException {
        TransferStrategy strategy = switch (args.length > 0 ? args[0] : "naive") {
            case "atomic" -> new AtomicTransfer();
            default -> new NaiveTransfer();
        };
        reset();
        long before = total();

        var ok = new AtomicInteger();
        var rejected = new AtomicInteger();
        var errors = new ConcurrentHashMap<String, LongAdder>();
        var start = new CountDownLatch(1);
        var threads = new ArrayList<Thread>();

        for (int w = 0; w < WORKERS; w++) {
            threads.add(Thread.ofPlatform().start(() -> {
                try(Connection connection = connect()) {
                    connection.setAutoCommit(false);
                    start.await();
                    var random = ThreadLocalRandom.current();
                    for (int i = 0; i < TRANSFERS_PER_WORKER; i++) {
                        long from = 1 + random.nextInt(ACCOUNTS);
                        long to = 1 + random.nextInt(ACCOUNTS);
                        if (from == to) continue;
                        try {
                            strategy.transfer(connection, from, to, 1 + random.nextInt(1_000));
                            connection.commit();
                            ok.incrementAndGet();
                        } catch (InsufficientFundsException e) {
                            connection.rollback();
                            rejected.incrementAndGet();
                        }
                         catch (SQLException e) {
                            connection.rollback();
                            errors.computeIfAbsent(e.getSQLState(), k -> new LongAdder()).increment();
                        }
                    }

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        long t0 = System.nanoTime();
        start.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;

        long after = total();
        System.out.printf("%s: %d ms, ok=%d, rejected=%d, errors=%s%n",
                strategy.getClass().getSimpleName(), ms, ok.get(), rejected.get(), errors);
        System.out.printf("invariant: before=%d after=%d -> %s%n",
                before, after, before == after ? "OK" : "BROKEN");
    }

    static void reset() throws SQLException {
        try (var connection = connect(); var statement = connection.createStatement()) {
            statement.execute("TRUNCATE lab_accounts");
            statement.execute("INSERT INTO lab_accounts (id, balance_minor) "
            + "SELECT g, " + INITIAL_BALANCE + " FROM generate_series(1, " + ACCOUNTS + ") g" );
        }
    }

    static long total() throws SQLException {
        try (var connection = connect(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT SUM(balance_minor) FROM lab_accounts");
            resultSet.next();
            return resultSet.getLong(1);
        }
    }


    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL,"kobo","kobo");
    }
}

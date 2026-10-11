package com.lehnade.kobo.lab;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class TransferRunner {
    static final String URL = "jdbc:postgresql://localhost:5432/kobo";
    static final int WORKERS = 16, TRANSFERS_PER_WORKER = 50;
    static final long INITIAL_BALANCE = 5_000;
    static final String RESULTS_TXT = "results.txt";

    public static void run(String[] args, List<TransferStrategy> strategies) throws SQLException, InterruptedException {
        int numberOfExecutions = Integer.parseInt(args[2]);
        for (int exec = 0; exec < numberOfExecutions; exec++) {
            writeToFile(String.format("==================== EXECUTION %d/%d ====================%n", exec + 1, numberOfExecutions));

            int ACCOUNTS = Integer.parseInt(args[1]);
            writeToFile(String.format("ACCOUNTS=%d, - INITIAL BALANCE=%d%n", ACCOUNTS, INITIAL_BALANCE));
            for (TransferStrategy strategy : strategies) {
                System.out.println("Running " + strategy.getClass().getSimpleName());

                writeToFile(">>>"+strategy.getClass().getSimpleName());

                reset(ACCOUNTS);
                long before = total();
                long negativeBalanceCountBefore = negativeBalanceCount();

                var ok = new AtomicInteger();
                var ignored = new AtomicInteger();
                var retries = new AtomicInteger();
                var finalFailures = new AtomicInteger();
                var rejected = new AtomicInteger();
                var errors = new ConcurrentHashMap<String, LongAdder>();
                var start = new CountDownLatch(1);
                var threads = new ArrayList<Thread>();
                var maxAttempts = 3;
                for (int w = 0; w < WORKERS; w++) {
                    threads.add(Thread.ofPlatform().start(() -> {
                        try(Connection connection = connect()) {
                            connection.setAutoCommit(false);
                            connection.setTransactionIsolation(strategy.isolationLevel());

                            start.await();
                            var random = ThreadLocalRandom.current();
                            for (int i = 0; i < TRANSFERS_PER_WORKER; i++) {
                                long from = 1 + random.nextInt(ACCOUNTS);
                                long to = 1 + random.nextInt(ACCOUNTS);
                                if (from == to) {
                                    ignored.incrementAndGet();
                                    continue;
                                }
                                long amount = 1 + random.nextInt(1_000);
                                for (int j = 0; j < maxAttempts; j++) {
                                    try {
                                        strategy.transfer(connection, from, to, amount);
                                        connection.commit();
                                        ok.incrementAndGet();
                                        break;
                                    } catch (InsufficientFundsException e) {
                                        connection.rollback();
                                        rejected.incrementAndGet();
                                        break;
                                    } catch (VersionConflictException e) {
                                        connection.rollback();
                                        retries.incrementAndGet();
                                        if (j == maxAttempts - 1) {
                                            finalFailures.incrementAndGet();
                                        }
                                    } catch (SQLException e) {
                                        connection.rollback();
                                        errors.computeIfAbsent(e.getSQLState(), k -> new LongAdder()).increment();
                                        if (isRetryable(e)) {
                                            retries.incrementAndGet();
                                            if (j == maxAttempts - 1) {
                                                finalFailures.incrementAndGet();
                                            }
                                        } else {
                                            break;
                                        }
                                    }
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
                long negativeBalanceCountAfter = negativeBalanceCount();

                writeToFile(String.format("%s: %d ms, ok=%d, ignored=%d, rejected=%d, errors=%s, failed attempts=%d, finalFailures=%d%n",
                        strategy.getClass().getSimpleName(), ms, ok.get(), ignored.get(), rejected.get(), errors, retries.get(), finalFailures.get()));
                writeToFile(String.format("invariant: before=%d after=%d negativeBalanceBefore=%d, negativeBalanceAfter=%d-> %s%n",
                        before, after, negativeBalanceCountBefore, negativeBalanceCountAfter, before == after ? "OK" : "BROKEN"));
            }
        }

    }

    static void reset(long ACCOUNTS) throws SQLException {
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

    static long negativeBalanceCount() throws SQLException {
        try (var connection = connect(); var statement = connection.createStatement()) {
            var resultSet = statement.executeQuery("SELECT count(*) FROM lab_accounts WHERE balance_minor < 0");
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL,"kobo","kobo");
    }

    static void writeToFile(String value) {
        try (FileOutputStream fos = new FileOutputStream(RESULTS_TXT, true)) {
            fos.write((value + "\n").getBytes());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isRetryable(SQLException e) {
        String state = e.getSQLState();
        return "40001".equals(state) || "40P01".equals(state);
    }
}

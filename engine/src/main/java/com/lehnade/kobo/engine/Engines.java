package com.lehnade.kobo.engine;

public final class Engines {
    private Engines() {}

    public static TransferEngine create(String name, int accounts, long initialBalance) {
        return switch (name) {
            case "naive" -> new NaiveEngine(accounts, initialBalance);
            case "globalLock" -> new GlobalLockEngine(accounts, initialBalance);
            case "unorderedLock" -> new UnorderedLockEngine(accounts, initialBalance);
            case "orderedLock" -> new OrderedLockEngine(accounts, initialBalance);
            default -> throw new IllegalArgumentException("Unknown engine: " + name);
        };
    }
}

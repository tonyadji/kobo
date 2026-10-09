package com.lehnade.kobo.lab;

import java.sql.Connection;
import java.sql.SQLException;

public class AtomicTransfer implements TransferStrategy {
    @Override
    public void transfer(Connection c, long from, long to, long amount) throws SQLException {
        var updatedRows = debit(c, from, amount);
        if (updatedRows == 0) {
            throw new InsufficientFundsException();
        }
        credit(c, to, amount);
    }

    static long debit(Connection c, long id, long amount) throws SQLException {
        try (var statement = c.prepareStatement("UPDATE lab_accounts SET balance_minor = balance_minor - ? WHERE id = ? AND balance_minor >= ?")) {
            statement.setLong(1, amount);
            statement.setLong(2, id);
            statement.setLong(3, amount);
            return statement.executeUpdate();
        }
    }

    static void credit(Connection c, long id, long amount) throws SQLException {
        try (var statement = c.prepareStatement("UPDATE lab_accounts SET balance_minor = balance_minor + ? WHERE id = ?")) {
            statement.setLong(1, amount);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }
}

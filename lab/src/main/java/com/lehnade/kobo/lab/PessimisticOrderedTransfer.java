package com.lehnade.kobo.lab;

import java.sql.Connection;
import java.sql.SQLException;

public class PessimisticOrderedTransfer implements TransferStrategy {
    @Override
    public void transfer(Connection c, long from, long to, long amount) throws SQLException {
        long fromBalance = 0;
        long toBalance = 0;
        if (from < to) {
            fromBalance = balance(c, from, amount);
            if (fromBalance < amount) {
                throw new InsufficientFundsException();
            }
            toBalance = balance(c, to, amount);
        } else {
            toBalance = balance(c, to, amount);
            fromBalance = balance(c, from, amount);
            if (fromBalance < amount) {
                throw new InsufficientFundsException();
            }
        }

        setBalance(c, from, fromBalance - amount);
        setBalance(c, to, toBalance + amount);
    }

    static long balance(Connection c, long id, long amount) throws SQLException {
        try (var statement = c.prepareStatement("SELECT balance_minor FROM lab_accounts WHERE id = ? FOR UPDATE")) {
            statement.setLong(1, id);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    static void setBalance(Connection c, long id, long balance) throws SQLException {
        try (var statement = c.prepareStatement("UPDATE lab_accounts SET balance_minor = ? WHERE id = ?")) {
            statement.setLong(1, balance);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

}

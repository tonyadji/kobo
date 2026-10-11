package com.lehnade.kobo.lab;

import java.sql.Connection;
import java.sql.SQLException;

public class OptimisticTransfer implements TransferStrategy {
    @Override
    public void transfer(Connection c, long from, long to, long amount) throws SQLException {
        BalanceAndVersion fromBalance = getBalanceAndVersion(c, from);
        BalanceAndVersion toBalance = getBalanceAndVersion(c, to);

        if (fromBalance.balance < amount) {
            throw new InsufficientFundsException();
        }
        if (from < to) {
            long updatedRows = setBalance(c, from, fromBalance.balance - amount, fromBalance.version());
            if (updatedRows == 0) {
                throw new VersionConflictException();
            }
            updatedRows = setBalance(c, to, toBalance.balance + amount, toBalance.version());
            if (updatedRows == 0) {
                throw new VersionConflictException();
            }
        } else {
            long updatedRows = setBalance(c, to, toBalance.balance + amount, toBalance.version());
            if (updatedRows == 0) {
                throw new VersionConflictException();
            }
            updatedRows = setBalance(c, from, fromBalance.balance - amount, fromBalance.version());
            if (updatedRows == 0) {
                throw new VersionConflictException();
            }
        }

    }

    static BalanceAndVersion getBalanceAndVersion(Connection c, long id) throws SQLException {
        try (var statement = c.prepareStatement("SELECT balance_minor, version FROM lab_accounts WHERE id = ?")) {
            statement.setLong(1, id);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return new BalanceAndVersion(resultSet.getLong(1), resultSet.getLong(2));
            }
        }
    }

    static long setBalance(Connection c, long id, long balance, long version) throws SQLException {
        try (var statement = c.prepareStatement("UPDATE lab_accounts SET balance_minor = ?, version = version + 1 WHERE id = ? AND version = ?")) {
            statement.setLong(1, balance);
            statement.setLong(2, id);
            statement.setLong(3, version);
            return statement.executeUpdate();
        }
    }

    record BalanceAndVersion(long balance, long version){
    }
}

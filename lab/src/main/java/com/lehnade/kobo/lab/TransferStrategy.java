package com.lehnade.kobo.lab;

import java.sql.Connection;
import java.sql.SQLException;

public interface TransferStrategy {
    void transfer(Connection c, long from, long to, long amount) throws SQLException;
}
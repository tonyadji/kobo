package com.lehnade.kobo;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

final class Postgres {
    static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:17");
    static  {
        CONTAINER.start();
        Flyway.configure()
                .dataSource(CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword())
                .load().migrate();
    }

    public static PGSimpleDataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(CONTAINER.getJdbcUrl());
        dataSource.setUser(CONTAINER.getUsername());
        dataSource.setPassword(CONTAINER.getPassword());
        return dataSource;
    }
    static void reset(DSLContext dsl) {
        dsl.execute("TRUNCATE transfers, accounts, transfers_idempotency_keys RESTART IDENTITY");
    }
}

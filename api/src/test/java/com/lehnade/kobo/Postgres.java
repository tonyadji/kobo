package com.lehnade.kobo;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

final class Postgres {
    static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:17");
    static  {
        CONTAINER.start();
        Flyway.configure()
                .dataSource(CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword())
                .load().migrate();
    }
}

package com.lehnade.kobo;

import org.testcontainers.containers.PostgreSQLContainer;

final class Postgres {
    static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:17");
    static  { CONTAINER.start(); }
}

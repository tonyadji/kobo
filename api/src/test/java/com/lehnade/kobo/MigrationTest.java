package com.lehnade.kobo;

import org.flywaydb.core.Flyway;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MigrationTest {

    private static final Table<?> ACCOUNT = DSL.table("accounts");
    private static final Field<Long> ACCOUNT_ID = DSL.field("id", Long.class);
    private static final Field<Long> ACCOUNT_BALANCE = DSL.field("balance_minor", Long.class);

    @Test
    void flywayMigratesEmptyPostgresAndAccountCanBeReadBack() {
        var url = Postgres.CONTAINER.getJdbcUrl();
        var username = Postgres.CONTAINER.getUsername();
        var password = Postgres.CONTAINER.getPassword();
        Flyway.configure()
            .dataSource(
                url,
                username,
                password)
            .load().migrate();

        try(var ctx = DSL.using(url, username, password)) {
            ctx.insertInto(ACCOUNT, ACCOUNT_ID, ACCOUNT_BALANCE)
                    .values(1L, 100L)
                    .execute();
            Long balance = ctx.select(ACCOUNT_BALANCE).from(ACCOUNT)
                    .where(ACCOUNT_ID.eq(1L))
                    .fetchOne(ACCOUNT_BALANCE);

            Assertions.assertEquals(100L, balance);
        }
    }
}

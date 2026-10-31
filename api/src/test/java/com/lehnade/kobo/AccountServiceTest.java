package com.lehnade.kobo;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AccountServiceTest {
    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static DSLContext dsl;

    @BeforeAll
    static void init() {
        dsl = DSL.using(Postgres.CONTAINER.getJdbcUrl(),
                Postgres.CONTAINER.getUsername(),
                Postgres.CONTAINER.getPassword());
    }

    @BeforeEach
    void cleanDatabase() {
        Postgres.reset(dsl);
    }

    @Test
    void getAccountWithValidIdShouldReturnAccount() {
        // arrange
        long id = 3L;
        long balanceMinor = 400;
        insertAccountsWithBalance(id, balanceMinor);

        // act
        var accountService = new AccountService(dsl);
        var result = accountService.getAccount(id);

        // assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(id, result.id());
        Assertions.assertEquals(balanceMinor, result.balance());
    }

    @Test
    void getAccountWithUnknowIdShouldThrowUnknownAccountException() {
        // arrange
        long id = 3L;

        // act
        var accountService = new AccountService(dsl);
        var exception = Assertions.assertThrows(UnknownAccountException.class,
                () -> accountService.getAccount(id));

        // assert
        Assertions.assertNotNull(exception);
        Assertions.assertNotNull("Account with id " + id + " not found",exception.getMessage());
    }

    private void insertAccountsWithBalance(long from, long balance) {
        dsl.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(from, balance)
                .execute();
    }
}

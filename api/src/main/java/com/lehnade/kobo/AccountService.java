package com.lehnade.kobo;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class AccountService {

    private static final Table<?> ACCOUNT_TABLE = DSL.table("accounts");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private final DSLContext ctx;

    public AccountService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public Account getAccount(long id) {
        return ctx.select(ID, BALANCE_MINOR).from(ACCOUNT_TABLE)
                .where(ID.eq(id))
                .fetchOptionalInto(Account.class)
                .orElseThrow(() -> new UnknownAccountException("Account with id " + id + " not found"));
    }
}

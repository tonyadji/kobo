package com.lehnade.kobo;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class TransferService {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private final DSLContext ctx;

    public TransferService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public void transfer(long from, long to, long amount) {
        validateAccount(from);
        validateAccount(to);
        if (from == to) { throw new InvalidTransferException("Self transfer is not allowed") ; }
        if (amount <= 0) { throw new InvalidTransferException("Amount should be greater than 0") ; }
        if (getBalance(from) < amount) { throw new UnsufficientFundException("Unsufficient balance") ; }
        debit(from, amount);
        credit(to, amount);
    }

    private void validateAccount(long account) {
        Long retrievedId = ctx.select(ID).from(ACCOUNTS)
                .where(ID.eq(account))
                .fetchOne(ID);
        if (retrievedId == null) throw new InvalidTransferException("Unknown account "+account);
    }

    private void debit(long from, long amount) {
        ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.minus(amount))
                .where(ID.eq(from))
                .execute();
    }

    private void credit(long to, long amount) {
        ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.plus(amount))
                .where(ID.eq(to))
                .execute();
    }

    private long getBalance(long id) {
        return ctx.select(BALANCE_MINOR).from(ACCOUNTS)
                .where(ID.eq(id))
                .fetchOne(BALANCE_MINOR);
    }
}

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
        if (from == to) { throw new InvalidTransferException("Self transfer is not allowed") ; }
        if (amount <= 0) { throw new InvalidTransferException("Amount should be greater than 0") ; }

        ctx.transaction(configuration -> {
            var txCtx = DSL.using(configuration);
            if (from < to) {
                debit(from, amount, txCtx);
                credit(to, amount, txCtx);
            } else {
                credit(to, amount, txCtx);
                debit(from, amount, txCtx);
            }
        });
    }

    private void requireAccountExists(long account, DSLContext ctx) {
        boolean accountExists = ctx.fetchExists(
                     ctx.selectOne().from(ACCOUNTS).where(ID.eq(account)));

        if (!accountExists) throw new UnknownAccountException("Unknown account "+account);
    }

    private void debit(long from, long amount, DSLContext ctx) {
        int updatedRow = ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.minus(amount))
                .where(ID.eq(from))
                .and(BALANCE_MINOR.ge(amount))
                .execute();
        if (updatedRow == 0) {
            requireAccountExists(from, ctx);
            throw new InsufficientFundException("Unsufficient balance");
        }
    }

    private void credit(long to, long amount, DSLContext ctx) {
        int updatedRow =  ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.plus(amount))
                .where(ID.eq(to))
                .execute();
        if (updatedRow == 0) { throw new UnknownAccountException("Unknown account "+to); }
    }
}

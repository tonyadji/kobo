package com.lehnade.kobo;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class TransferService {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Table<?> TRANSFERS = DSL.table("transfers");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static final Field<Long> AMOUNT_MINOR = DSL.field("amount_minor", Long.class);
    private static final Field<Long> FROM_ACCOUNT = DSL.field("from_account_id", Long.class);
    private static final Field<Long> TO_ACCOUNT = DSL.field("to_account_id", Long.class);
    private final DSLContext ctx;

    public TransferService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public long transfer(long from, long to, long amount) {
        if (from == to) { throw new InvalidTransferException("Self transfer is not allowed") ; }
        if (amount <= 0) { throw new InvalidTransferException("Amount should be greater than 0") ; }

        long transferId = ctx.transactionResult(configuration -> {
            var txCtx = DSL.using(configuration);
            if (from < to) {
                debit(from, amount, txCtx);
                credit(to, amount, txCtx);
            } else {
                credit(to, amount, txCtx);
                debit(from, amount, txCtx);
            }

            return transfer(from, to, amount, txCtx);
        });
        return transferId;
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

    private long transfer(long from, long to, long amount, DSLContext ctx) {
        return ctx.insertInto(TRANSFERS, FROM_ACCOUNT, TO_ACCOUNT, AMOUNT_MINOR)
                .values(from, to, amount)
                .returning(ID)
                .fetchOne(ID);
    }
}

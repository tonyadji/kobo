package com.lehnade.kobo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lehnade.kobo.application.TransferOutcome;
import com.lehnade.kobo.domain.*;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.List;

public class TransferService {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Table<?> TRANSFERS = DSL.table("transfers");
    private static final Table<?> IDEMPOTENCY_KEYS = DSL.table("transfers_idempotency_keys");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static final Field<Long> AMOUNT_MINOR = DSL.field("amount_minor", Long.class);
    private static final Field<Long> FROM_ACCOUNT = DSL.field("from_account_id", Long.class);
    private static final Field<Long> TO_ACCOUNT = DSL.field("to_account_id", Long.class);
    private static final Field<String> KEY = DSL.field("key", String.class);
    private static final Field<String> REQUEST_HASH = DSL.field("request_hash", String.class);
    private static final Field<Long> TRANSFER_ID = DSL.field("transfer_id", Long.class);
    private static final Field<String> OUTCOME = DSL.field("outcome", String.class);
    private static final Field<Long> ACCOUNT_ID = DSL.field("account_id", Long.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    protected static final String UNKNOWN_ACCOUNT = "Unknown account ";
    private final DSLContext ctx;

    public TransferService(DSLContext ctx) {
        this.ctx = ctx;
    }

    public TransferOutcome transfer(long from, long to, long amount, IdempotencyKey key) {
        Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount));
        TransferOutcome transactionResponse = ctx.transactionResult(configuration -> {
            var outerTx = DSL.using(configuration);
            int insertedRows = insertIdempotencyKey(key, outerTx);
            if (insertedRows == 0) {
                return handleExistingIdempotencyKey(key, outerTx);
            }
            try {
                return outerTx.transactionResult(innerConfig -> {
                    var innerTx = DSL.using(innerConfig);
                    if (from < to) {
                        debit(from, amount, innerTx);
                        credit(to, amount, innerTx);
                    } else {
                        credit(to, amount, innerTx);
                        debit(from, amount, innerTx);
                    }
                    long transferIdentifier = transfer(from, to, amount, innerTx);
                    updateIdempotencyKey(key.key(), transferIdentifier, "COMPLETED", null, innerTx);

                    return new TransferOutcome.Completed(new TransferId(transferIdentifier));
                });
            } catch (UnknownAccountException e) {
                updateIdempotencyKey(key.key(), null, "UNKNOWN_ACCOUNT", e.getAccountId(), outerTx);
                return new TransferOutcome.Rejected(new TransferRejection.UnknownAccount(new AccountId(e.getAccountId())));
            } catch (InsufficientFundException e) {
                updateIdempotencyKey(key.key(), null, "INSUFFICIENT_FUNDS", null, outerTx);
                return new TransferOutcome.Rejected(new TransferRejection.InsufficientFunds());
            }
        });
        return transactionResponse;
    }

    private long updateIdempotencyKey(String key, Long transferId, String outcome, Long accountId, DSLContext ctx) {
        return ctx.update(IDEMPOTENCY_KEYS)
                .set(TRANSFER_ID, transferId)
                .set(OUTCOME, outcome)
                .set(ACCOUNT_ID, accountId)
                .where(KEY.eq(key))
                .execute();
    }

    private void requireAccountExists(long account, DSLContext ctx) {
        boolean accountExists = ctx.fetchExists(
                ctx.selectOne().from(ACCOUNTS).where(ID.eq(account)));

        if (!accountExists) throw new UnknownAccountException(UNKNOWN_ACCOUNT, account);
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
        int updatedRow = ctx.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.plus(amount))
                .where(ID.eq(to))
                .execute();
        if (updatedRow == 0) {
            throw new UnknownAccountException(UNKNOWN_ACCOUNT, to);
        }
    }

    private long transfer(long from, long to, long amount, DSLContext ctx) {
        return ctx.insertInto(TRANSFERS, FROM_ACCOUNT, TO_ACCOUNT, AMOUNT_MINOR)
                .values(from, to, amount)
                .returning(ID)
                .fetchOne(ID);
    }

    private int insertIdempotencyKey(IdempotencyKey key, DSLContext ctx) {
        return ctx.insertInto(IDEMPOTENCY_KEYS, KEY, REQUEST_HASH, OUTCOME)
                .values(key.key(), key.requestHash(), key.outcome())
                .onConflict(List.of(KEY))
                .doNothing()
                .execute();
    }

    private IdempotencyKey getKey(String key, DSLContext ctx) {
        return ctx.select(KEY, REQUEST_HASH, OUTCOME, TRANSFER_ID, ACCOUNT_ID).from(IDEMPOTENCY_KEYS)
                .where(KEY.eq(key))
                .fetchOneInto(IdempotencyKey.class);
    }

    private TransferOutcome handleExistingIdempotencyKey(IdempotencyKey key, DSLContext ctx) {
        IdempotencyKey existingKey = getKey(key.key(), ctx);
        if (!key.requestHash().equals(existingKey.requestHash())) {
            return new TransferOutcome.KeyReused();
        }
        return switch (existingKey.outcome()) {
            case "PROCESSING" -> new TransferOutcome.Processing();
            case "COMPLETED" -> new TransferOutcome.Completed(new TransferId(existingKey.transferId()));
            case "INSUFFICIENT_FUNDS" -> new TransferOutcome.Rejected(new TransferRejection.InsufficientFunds());
            case "UNKNOWN_ACCOUNT" -> new TransferOutcome.Rejected(new TransferRejection.UnknownAccount(new AccountId(existingKey.accountId())));
            default -> throw new IllegalStateException("Unexpected value: " + existingKey.outcome());
        };
    }

}

package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.IdempotencyKey;
import com.lehnade.kobo.application.IdempotencyStore;
import com.lehnade.kobo.application.TransferOutcome;
import com.lehnade.kobo.domain.TransferRejection;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;

public class JooqIdempotencyStore implements IdempotencyStore {

    private static final Table<?> IDEMPOTENCY_KEYS = DSL.table("transfers_idempotency_keys");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<String> KEY = DSL.field("key", String.class);
    private static final Field<String> REQUEST_HASH = DSL.field("request_hash", String.class);
    private static final Field<Long> TRANSFER_ID = DSL.field("transfer_id", Long.class);
    private static final Field<String> OUTCOME = DSL.field("outcome", String.class);
    private static final Field<Long> ACCOUNT_ID = DSL.field("account_id", Long.class);

    private DSLContext dslContext;

    public JooqIdempotencyStore(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public boolean insert(IdempotencyKey key) {
        int insertedRows = dslContext.insertInto(IDEMPOTENCY_KEYS, KEY, REQUEST_HASH, OUTCOME)
                .values(key.key(), key.requestHash(), key.outcome())
                .onConflict(List.of(KEY))
                .doNothing()
                .execute();
        return insertedRows > 0;
    }

    @Override
    public void complete(IdempotencyKey key, TransferOutcome outcome) {
        dslContext.update(IDEMPOTENCY_KEYS)
                .set(TRANSFER_ID, transferId(outcome))
                .set(OUTCOME, ofOutcome(outcome))
                .set(ACCOUNT_ID, accountId(outcome))
                .where(KEY.eq(key.key()))
                .execute();
    }

    @Override
    public Optional<IdempotencyKey> find(String key) {
        return dslContext.select(KEY, REQUEST_HASH, OUTCOME, TRANSFER_ID, ACCOUNT_ID).from(IDEMPOTENCY_KEYS)
                .where(KEY.eq(key))
                .fetchOptionalInto(IdempotencyKey.class);
    }

    private Long transferId(TransferOutcome outcome) {
        if (outcome instanceof TransferOutcome.Completed completed) {
            return completed.id().value();
        }
        return null;
    }

    private Long accountId(TransferOutcome outcome) {
        if (outcome instanceof TransferOutcome.Rejected rejected) {
            if (rejected.rejection() instanceof TransferRejection.UnknownAccount unknownAccount) {
                return unknownAccount.id().value();
            }
        }
        return null;
    }

    private String ofOutcome(TransferOutcome outcome) {
        return switch (outcome) {
            case TransferOutcome.Completed completed -> "COMPLETED";
            case TransferOutcome.Rejected rejected -> switch (rejected.rejection()) {
                case TransferRejection.InsufficientFunds insufficientFunds -> "INSUFFICIENT_FUNDS";
                case TransferRejection.UnknownAccount unknownAccount -> "UNKNOWN_ACCOUNT";
            };
            default -> throw new IllegalStateException("Unexpected outcome");
        };
    }
}

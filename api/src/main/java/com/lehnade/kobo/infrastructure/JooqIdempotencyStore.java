package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.IdempotencyKey;
import com.lehnade.kobo.application.IdempotencyStore;
import com.lehnade.kobo.application.TransferOutcome;
import org.jooq.DSLContext;

import java.util.Optional;

public class JooqIdempotencyStore implements IdempotencyStore {

    private DSLContext dslContext;

    public JooqIdempotencyStore(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public boolean insert(IdempotencyKey key) {
        return false;
    }

    @Override
    public void complete(IdempotencyKey key, TransferOutcome outcome) {

    }

    @Override
    public Optional<IdempotencyKey> find(String key) {
        return Optional.empty();
    }
}

package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.application.IdempotencyStore;
import com.lehnade.kobo.application.UnitOfWork;
import com.lehnade.kobo.domain.AccountRepository;
import com.lehnade.kobo.domain.TransferRepository;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.function.Function;

public class JooqUnitOfWork implements UnitOfWork {

    private final DSLContext dslContext;

    public JooqUnitOfWork(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public AccountRepository accounts() {
        return new JooqAccountRepository(dslContext);
    }

    @Override
    public TransferRepository transfers() {
        return new JooqTransferRepository(dslContext);
    }

    @Override
    public IdempotencyStore keys() {
        return new JooqIdempotencyStore(dslContext);
    }

    @Override
    public <T> T savepoint(Function<UnitOfWork, T> work) {
        return dslContext.transactionResult(config ->
                work.apply(new JooqUnitOfWork(DSL.using(config)))
        );
    }
}

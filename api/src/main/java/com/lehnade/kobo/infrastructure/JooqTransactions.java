package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.application.Transactions;
import com.lehnade.kobo.application.UnitOfWork;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.util.function.Function;

public class JooqTransactions implements Transactions {

    private final DSLContext dslContext;
    public JooqTransactions(DSLContext dslContext) {
        this.dslContext = dslContext;
    }
    @Override
    public <T> T inTransaction(Function<UnitOfWork, T> work) {
        return dslContext.transactionResult(config ->
            work.apply(new JooqUnitOfWork(DSL.using(config)))
        );
    }
}

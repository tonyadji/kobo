package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.domain.Transfer;
import com.lehnade.kobo.domain.TransferId;
import com.lehnade.kobo.domain.TransferRepository;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class JooqTransferRepository implements TransferRepository {

    private static final Table<?> TRANSFERS = DSL.table("transfers");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> AMOUNT_MINOR = DSL.field("amount_minor", Long.class);
    private static final Field<Long> FROM_ACCOUNT = DSL.field("from_account_id", Long.class);
    private static final Field<Long> TO_ACCOUNT = DSL.field("to_account_id", Long.class);


    private final DSLContext dslContext;

    public JooqTransferRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public TransferId transfer(Transfer transfer) {
        long id = dslContext.insertInto(TRANSFERS, FROM_ACCOUNT, TO_ACCOUNT, AMOUNT_MINOR)
                .values(transfer.from().value(), transfer.to().value(), transfer.amount().amountMinor())
                .returning(ID)
                .fetchOne(ID);
        return new TransferId(id);
    }
}

package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.domain.Transfer;
import com.lehnade.kobo.domain.TransferId;
import com.lehnade.kobo.domain.TransferRepository;
import org.jooq.DSLContext;

public class JooqTransferRepository implements TransferRepository {
    private final DSLContext dslContext;

    public JooqTransferRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public TransferId transfer(Transfer transfer) {
        return null;
    }
}

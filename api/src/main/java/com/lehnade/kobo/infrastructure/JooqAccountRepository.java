package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.domain.AccountId;
import com.lehnade.kobo.domain.AccountRepository;
import com.lehnade.kobo.domain.Money;
import org.jooq.DSLContext;

public class JooqAccountRepository implements AccountRepository {

    private final DSLContext dslContext;

    public JooqAccountRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public boolean debitIfSufficient(AccountId accountId, Money amount) {
        return false;
    }

    @Override
    public boolean credit(AccountId accountId, Money amount) {
        return false;
    }

    @Override
    public boolean exists(AccountId accountId) {
        return false;
    }
}

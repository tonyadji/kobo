package com.lehnade.kobo.infrastructure;

import com.lehnade.kobo.domain.AccountId;
import com.lehnade.kobo.domain.AccountRepository;
import com.lehnade.kobo.domain.Money;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class JooqAccountRepository implements AccountRepository {
    private static final Table<?> ACCOUNTS = DSL.table("accounts");

    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private final DSLContext dslContext;

    public JooqAccountRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public boolean debitIfSufficient(AccountId accountId, Money amount) {
        int updatedRow = dslContext.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.minus(amount.amountMinor()))
                .where(ID.eq(accountId.value()))
                .and(BALANCE_MINOR.ge(amount.amountMinor()))
                .execute();
        return updatedRow > 0;
    }

    @Override
    public boolean credit(AccountId accountId, Money amount) {
        int updatedRow = dslContext.update(ACCOUNTS)
                .set(BALANCE_MINOR, BALANCE_MINOR.plus(amount.amountMinor()))
                .where(ID.eq(accountId.value()))
                .execute();
        return updatedRow > 0;
    }

    @Override
    public boolean exists(AccountId accountId) {
        return dslContext.fetchExists(dslContext.selectOne().from(ACCOUNTS).where(ID.eq(accountId.value())));
    }
}

package com.lehnade.kobo;

import com.lehnade.kobo.application.IdempotentTransfer;
import com.lehnade.kobo.application.TransferMoney;
import com.lehnade.kobo.domain.AccountId;
import com.lehnade.kobo.domain.DomainException;
import com.lehnade.kobo.domain.Money;
import com.lehnade.kobo.domain.Transfer;
import com.lehnade.kobo.infrastructure.JooqTransactions;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeContainer;
import net.jqwik.api.statistics.Statistics;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;

public class TransferPropertiesTest {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);

    private static DSLContext ctx;

    @BeforeContainer
    static void setUp() {
        ctx = DSL.using(Postgres.dataSource(), SQLDialect.POSTGRES);
    }

    @Property(tries = 100)
    void sumOfAllBalanceIsConstantAfterTransferAndNoBalanceIsNegative(
            @ForAll("transfers") List<GeneratedTransfer> transfers,
            @ForAll("accounts") List<Account> accounts
    ) {
        init(accounts);

        long sumBefore = sumBalances();

        IdempotentTransfer transferService = new IdempotentTransfer(new TransferMoney(), new JooqTransactions(ctx));
        for (GeneratedTransfer transfer : transfers) {
            try {
                var response = transferService.execute(transfer.toDomainTransfer(), keyOf(transfer));

            } catch (KoboException | DomainException e) {
                Statistics.collect(e.getClass().getSimpleName());
            }

        }

        long sumAfter = sumBalances();

        long negativeBalanceCount = accountsWithNegativeBalance();

        Assertions.assertEquals(0, negativeBalanceCount);
        Assertions.assertEquals(sumBefore, sumAfter);
    }

    private IdempotencyKey keyOf(GeneratedTransfer transfer) {
        return IdempotencyKey.of(transfer.key(), HashHelper.hash(transfer));
    }

    @Provide
    Arbitrary<List<GeneratedTransfer>> transfers() {
        Arbitrary<Long> account = Arbitraries.longs().between(0, 5)
                .withDistribution(RandomDistribution.uniform());
        Arbitrary<Long> amount = Arbitraries.frequencyOf(
                Tuple.of(9, Arbitraries.longs().between(1, 3_000).withDistribution(RandomDistribution.uniform())),
                Tuple.of(1, Arbitraries.longs().between(-1_000, 0)));

        Arbitrary<GeneratedTransfer> transfer = Combinators.combine(account, account, amount,
                Arbitraries.strings().alpha().ofLength(8)).as(GeneratedTransfer::new);

        return transfer.list().ofMinSize(1).ofMaxSize(20)
                .withSizeDistribution(RandomDistribution.uniform());
    }

    @Provide
    Arbitrary<List<Account>> accounts() {
        List<Arbitrary<Account>> accounts = new ArrayList<>();
        for (long i = 0; i < 5; i++) {
            long id = i;
            accounts.add(Arbitraries.longs().between(0,10_000)
                    .withDistribution(RandomDistribution.uniform())
                    .map(balance -> new Account(id, balance)));
        }
        return Combinators.combine(accounts).as(List::copyOf);
    }

    private void init(List<Account> accounts) {
        ctx.execute("TRUNCATE transfers, accounts, transfers_idempotency_keys RESTART IDENTITY");
        accounts.forEach(a -> ctx.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(a.id(), a.balance())
                .execute());
    }

    private long sumBalances() {
        return ctx.select(DSL.sum(BALANCE_MINOR)).from(ACCOUNTS).fetchOne(0, Long.class);
    }

    private long accountsWithNegativeBalance() {
        return ctx.selectCount().from(ACCOUNTS)
                .where(BALANCE_MINOR.lt(0L))
                .fetchOne(0, Long.class);
    }
    record Account(long id, long balance) {}
    record GeneratedTransfer(long from, long to, long amount, String key) {
        public Transfer toDomainTransfer() {
            return Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount));
        }
    }
}

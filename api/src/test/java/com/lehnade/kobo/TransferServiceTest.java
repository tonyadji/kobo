package com.lehnade.kobo;

import com.lehnade.kobo.application.IdempotentTransfer;
import com.lehnade.kobo.application.TransferMoney;
import com.lehnade.kobo.application.TransferOutcome;
import com.lehnade.kobo.domain.*;
import com.lehnade.kobo.infrastructure.JooqTransactions;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class TransferServiceTest {

    private static final Table<?> ACCOUNTS = DSL.table("accounts");
    private static final Field<Long> ID = DSL.field("id", Long.class);
    private static final Field<Long> BALANCE_MINOR = DSL.field("balance_minor", Long.class);
    private static DSLContext dsl;

    @BeforeAll
    static void init() {
        dsl = DSL.using(Postgres.dataSource(), SQLDialect.POSTGRES);
    }

    @BeforeEach
    void cleanDatabase() {
        Postgres.reset(dsl);
    }

    @Test
    void aValidTransferMustDebitFromAccountAndCreditToAccountAndTheSumOfBalancesIsTheSame() {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 100L;
        insertAccountsWithBalance(from, 200L);
        insertAccountsWithBalance(to, 150L);
        long balanceFromBefore = getBalance(from);
        long balanceToBefore = getBalance(to);
        long totalBalanceBefore = balanceFromBefore + balanceToBefore;
        IdempotencyKey key = keyOf(from, to, amount);
        // act
        IdempotentTransfer transferService = new IdempotentTransfer(new TransferMoney(), new JooqTransactions(dsl));
        Transfer transfer = Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount));
        transferService.execute(transfer, key);

        // assert
        long balanceFromAfter = getBalance(from);
        long balanceToAfter = getBalance(to);
        long totalBalanceAfter = balanceFromAfter + balanceToAfter;

        Assertions.assertEquals(totalBalanceBefore, totalBalanceAfter);
        Assertions.assertEquals(balanceFromBefore - amount, balanceFromAfter);
        Assertions.assertEquals(balanceToBefore + amount, balanceToAfter);

    }

    @Test
    void unsufficientBalanceIsRefused() {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 300L;
        insertAccountsWithBalance(from, 200L);
        insertAccountsWithBalance(to, 150L);
        long balanceFromBefore = getBalance(from);
        long balanceToBefore = getBalance(to);
        long totalBalanceBefore = balanceFromBefore + balanceToBefore;
        Transfer transfer = Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount));
        IdempotencyKey key = keyOf(from, to, amount);

        // act
        IdempotentTransfer transferService = new IdempotentTransfer(new TransferMoney(), new JooqTransactions(dsl));
        TransferOutcome outcome = transferService.execute(transfer, key);

        // assert
        long balanceFromAfter = getBalance(from);
        long balanceToAfter = getBalance(to);
        long totalBalanceAfter = balanceFromAfter + balanceToAfter;

        Assertions.assertTrue(outcome instanceof TransferOutcome.Rejected);
        TransferOutcome.Rejected rejected = (TransferOutcome.Rejected) outcome;
        Assertions.assertTrue(rejected.rejection() instanceof TransferRejection.InsufficientFunds);
        Assertions.assertEquals(totalBalanceBefore, totalBalanceAfter);
        Assertions.assertEquals(balanceFromBefore, balanceFromAfter);
        Assertions.assertEquals(balanceToBefore, balanceToAfter);
    }

    @Test
    void amountLessOrEqualToZeroIsRefused() {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 0;
        insertAccountsWithBalance(from, 200L);
        insertAccountsWithBalance(to, 150L);
        long balanceFromBefore = getBalance(from);
        long balanceToBefore = getBalance(to);
        long totalBalanceBefore = balanceFromBefore + balanceToBefore;
        IdempotencyKey key = keyOf(from, to, amount);
        // act
        DomainException exception = Assertions.assertThrows(DomainException.class,
                () -> Money.of(amount));

        // assert
        long balanceFromAfter = getBalance(from);
        long balanceToAfter = getBalance(to);
        long totalBalanceAfter = balanceFromAfter + balanceToAfter;

        Assertions.assertEquals("Amount minor must be positive", exception.getMessage());
        Assertions.assertEquals(totalBalanceBefore, totalBalanceAfter);
        Assertions.assertEquals(balanceFromBefore, balanceFromAfter);
        Assertions.assertEquals(balanceToBefore, balanceToAfter);
    }

    @Test
    void selfTransferIsRefused() {
        // arrange
        long from = 2L;
        long to = 2L;
        long amount = 20;
        insertAccountsWithBalance(from, 200L);
        long balanceFromBefore = getBalance(from);
        long totalBalanceBefore = balanceFromBefore;
        IdempotencyKey key = keyOf(from, to, amount);

        // act
        DomainException exception = Assertions.assertThrows(DomainException.class,
                () -> Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount)));

        // assert
        long balanceFromAfter = getBalance(from);
        long totalBalanceAfter = balanceFromAfter;

        Assertions.assertEquals("Self transfer not allowed", exception.getMessage());
        Assertions.assertEquals(totalBalanceBefore, totalBalanceAfter);
        Assertions.assertEquals(balanceFromBefore, balanceFromAfter);
    }

    @Test
    void unknownToAccountIsRefused() {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 10;
        insertAccountsWithBalance(from, 200L);

        long balanceFromBefore = getBalance(from);
        IdempotencyKey key = keyOf(from, to, amount);
        Transfer transfer = Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount));
        // act
        IdempotentTransfer transferService = new IdempotentTransfer(new TransferMoney(), new JooqTransactions(dsl));
        TransferOutcome outcome = transferService.execute(transfer, key);

        // assert
        long balanceFromAfter = getBalance(from);

        Assertions.assertTrue(outcome instanceof TransferOutcome.Rejected);
        TransferOutcome.Rejected rejected = (TransferOutcome.Rejected) outcome;
        Assertions.assertTrue(rejected.rejection() instanceof TransferRejection.UnknownAccount);
        Assertions.assertEquals(balanceFromBefore, balanceFromAfter);
    }

    @Test
    void unknownFromAccountIsRefused() {
        // arrange
        long from = 2L;
        long to = 3L;
        long amount = 10;
        IdempotencyKey key = keyOf(from, to, amount);
        Transfer transfer = Transfer.between(new AccountId(from), new AccountId(to), Money.of(amount));
        // act
        IdempotentTransfer transferService = new IdempotentTransfer(new TransferMoney(), new JooqTransactions(dsl));
        TransferOutcome outcome = transferService.execute(transfer, key);

        // assert
        Assertions.assertTrue(outcome instanceof TransferOutcome.Rejected);
        TransferOutcome.Rejected rejected = (TransferOutcome.Rejected) outcome;
        Assertions.assertTrue(rejected.rejection() instanceof TransferRejection.UnknownAccount);
    }

    private void insertAccountsWithBalance(long from, long balance) {
        dsl.insertInto(ACCOUNTS, ID, BALANCE_MINOR)
                .values(from, balance)
                .execute();
    }

    private long getBalance(long id) {
        return dsl.select(BALANCE_MINOR).from(ACCOUNTS)
                .where(ID.eq(id))
                .fetchOne(BALANCE_MINOR);
    }

    private IdempotencyKey keyOf(long from, long to, long amount) {
        var request = new TransferRequestDto(from, to, amount);
        return IdempotencyKey.of(UUID.randomUUID().toString(), HashHelper.hash(request));
    }

}

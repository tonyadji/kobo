package com.lehnade.kobo.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransferTest {

    private static final AccountId FROM = new AccountId(1);
    private static final AccountId TO = new AccountId(2);
    private static final Money AMOUNT = new Money(200, SupportedCurrency.EUR);

    @Test
    void transferWithNullAmountShouldThrowException() {
        // arrange act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Transfer.between(FROM, TO, null));

        // assert
        assertNotNull(exception);
        assertEquals("Invalid amount", exception.getMessage());
    }

    @Test
    void transferWithNullFromAccountShouldThrowException() {
        // arrange act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Transfer.between(null, TO, null));

        // assert
        assertNotNull(exception);
        assertEquals("Invalid account", exception.getMessage());
    }

    @Test
    void transferWithNullToAccountShouldThrowException() {
        // arrange act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Transfer.between(FROM, null, null));

        // assert
        assertNotNull(exception);
        assertEquals("Invalid account", exception.getMessage());
    }

    @Test
    void selfTransferShouldThrowException() {
        // arrange act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Transfer.between(new AccountId(1), new AccountId(1), AMOUNT));

        // assert
        assertNotNull(exception);
        assertEquals("Self transfer not allowed", exception.getMessage());
    }

    @Test
    void validTransferShouldCreateInstanceAndReturnCorrectValues() {
        // arrange act
        Transfer transfer = Transfer.between(FROM, TO, AMOUNT);

        // assert
        assertNotNull(transfer);
        assertEquals(FROM, transfer.from());
        assertEquals(TO, transfer.to());
        assertEquals(AMOUNT, transfer.amount());
    }
}
package com.lehnade.kobo.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void moneyWithNegativeAmountShouldThrowException() {
        // arrange & act
        DomainException exception = Assertions.assertThrows(DomainException.class,
                () -> new Money(-20, SupportedCurrency.EUR));

        // assert
        assertNotNull(exception);
        assertEquals("Amount minor must be positive", exception.getMessage());
    }

    @Test
    void moneyWith0AmountShouldThrowException() {
        // arrange & act
        DomainException exception = Assertions.assertThrows(DomainException.class,
                () -> new Money(0, SupportedCurrency.EUR));

        // assert
        assertNotNull(exception);
        assertEquals("Amount minor must be positive", exception.getMessage());
    }

    @Test
    void moneyWithNullCurrencyShouldThrowException() {
        DomainException exception = Assertions.assertThrows(DomainException.class,
                () -> new Money(10, null));

        // assert
        assertNotNull(exception);
        assertEquals("Invalid currency", exception.getMessage());
    }

    @Test
    void validCurrencyShouldCreateInstanceAndReturnCorrectValues() {
        // arrange act
        Money money = new Money(1000, SupportedCurrency.EUR);

        // assert
        assertNotNull(money);
        assertEquals(1000, money.amountMinor());
        assertEquals(SupportedCurrency.EUR, money.currency());
    }
}
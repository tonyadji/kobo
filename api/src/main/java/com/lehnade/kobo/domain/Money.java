package com.lehnade.kobo.domain;


public record Money(long amountMinor, SupportedCurrency currency) {

    public Money {
        if (amountMinor <= 0) throw new DomainException("Amount minor must be positive");
        if (currency == null) throw new DomainException("Invalid currency");
    }

    public static Money of(long amount) {
        return new Money(amount, SupportedCurrency.EUR);
    }
}

package com.lehnade.kobo.domain;


public record Money(long amountMinor, SupportedCurrency currency) {

    public Money {
        if (amountMinor <= 0) throw new IllegalArgumentException("Amount minor must be positive");
        if (currency == null) throw new IllegalArgumentException("Invalid currency");
    }

}

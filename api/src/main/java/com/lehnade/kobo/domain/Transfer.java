package com.lehnade.kobo.domain;

public record Transfer(AccountId from, AccountId to, Money amount) {

    public Transfer {
        if (from == null || to == null) throw new DomainException("Invalid account");
        if (from.equals(to)) throw new DomainException("Self transfer not allowed");
        if (amount == null) throw new DomainException("Invalid amount");
    }

    public static Transfer between(AccountId from, AccountId to, Money amount) {
        return new Transfer(from, to, amount);
    }
}

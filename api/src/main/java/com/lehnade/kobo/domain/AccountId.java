package com.lehnade.kobo.domain;

public record AccountId(long value) {

    public AccountId {
        if (value <= 0) throw new IllegalArgumentException("Invalid id");
    }
}

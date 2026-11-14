package com.lehnade.kobo.domain;

public record TransferId(long value) {
    public TransferId {
        if (value <= 0) throw new DomainException("Invalid id");
    }
}

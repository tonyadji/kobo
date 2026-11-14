package com.lehnade.kobo.application;

import com.lehnade.kobo.domain.AccountRepository;
import com.lehnade.kobo.domain.TransferRepository;

import java.util.function.Function;

public interface UnitOfWork {
    AccountRepository accounts();
    TransferRepository transfers();
    IdempotencyStore keys();
    <T> T savepoint(Function<UnitOfWork, T> work);
}

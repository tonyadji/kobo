package com.lehnade.kobo.application;

import java.util.function.Function;

public interface Transactions {
    <T> T inTransaction(Function<UnitOfWork, T> work);
}

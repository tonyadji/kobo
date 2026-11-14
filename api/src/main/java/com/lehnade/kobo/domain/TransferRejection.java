package com.lehnade.kobo.domain;

public sealed interface TransferRejection {

    record InsufficientFunds() implements  TransferRejection {}
    record UnknownAccount(AccountId id) implements  TransferRejection {}
    record ConflictingIdempotencyKey() implements  TransferRejection {}
    record ProcessingIdempotencyKey() implements  TransferRejection {}
}

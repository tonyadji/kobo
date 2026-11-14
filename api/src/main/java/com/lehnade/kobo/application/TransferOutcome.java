package com.lehnade.kobo.application;

import com.lehnade.kobo.domain.TransferId;
import com.lehnade.kobo.domain.TransferRejection;

public sealed interface TransferOutcome {

    record Completed(TransferId id) implements  TransferOutcome {}
    record Rejected(TransferRejection rejection) implements  TransferOutcome {}
    record Processing() implements  TransferOutcome {}

    default String outcome() {
        return switch (this) {
            case Completed completed -> "COMPLETED";
            case Rejected rejected -> switch (rejected.rejection()) {
                case TransferRejection.InsufficientFunds insufficientFunds -> "INSUFFICIENT_FUNDS";
                case TransferRejection.UnknownAccount unknownAccount -> "UNKNOWN_ACCOUNT";
                case TransferRejection.ConflictingIdempotencyKey cik -> "CONFLICTING_IDEMPOTENCY_KEY";
                case TransferRejection.ProcessingIdempotencyKey pi -> "PROCESSING";
            };
            case Processing processing -> "PROCESSING";
        };
    }
}

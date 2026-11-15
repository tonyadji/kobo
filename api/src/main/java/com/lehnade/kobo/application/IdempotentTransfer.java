package com.lehnade.kobo.application;

import com.lehnade.kobo.IdempotencyKey;
import com.lehnade.kobo.domain.AccountId;
import com.lehnade.kobo.domain.Transfer;
import com.lehnade.kobo.domain.TransferId;
import com.lehnade.kobo.domain.TransferRejection;

import java.util.Optional;

public class IdempotentTransfer {

    private final TransferMoney transferMoney;
    private final Transactions transaction;

    public IdempotentTransfer(TransferMoney transferMoney, Transactions transaction) {
        this.transferMoney = transferMoney;
        this.transaction = transaction;
    }

    public TransferOutcome execute(Transfer transfer, IdempotencyKey key) {
        return transaction.inTransaction(uow -> {
            if (!uow.keys().insert(key)) {
                return existing(uow.keys().find(key.key()), key);
            }
            var outcome = transferMoney.execute(uow, transfer);
            uow.keys().complete(key, outcome);
            return outcome;
        });
    }

    private TransferOutcome existing(Optional<IdempotencyKey> optionalIdempotencyKey, IdempotencyKey key) {
        var existingKey = optionalIdempotencyKey.orElseThrow();
        if (!key.requestHash().equals(existingKey.requestHash())) {
            return new TransferOutcome.KeyReused();
        }
        return switch (existingKey.outcome()) {
            case "PROCESSING" -> new TransferOutcome.Processing();
            case "COMPLETED" -> new TransferOutcome.Completed(new TransferId(existingKey.transferId()));
            case "INSUFFICIENT_FUNDS" -> new TransferOutcome.Rejected(new TransferRejection.InsufficientFunds());
            case "UNKNOWN_ACCOUNT" -> new TransferOutcome.Rejected(new TransferRejection.UnknownAccount(new AccountId(existingKey.accountId())));
            default -> throw new IllegalStateException("Unexpected value: " + existingKey.outcome());
        };
    }
}

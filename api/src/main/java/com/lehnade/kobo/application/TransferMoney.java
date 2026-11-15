package com.lehnade.kobo.application;

import com.lehnade.kobo.domain.*;

public class TransferMoney {


    public TransferMoney() {
    }

    TransferOutcome execute(UnitOfWork uow, Transfer transfer) {
        try {
            return uow.savepoint(tx -> {
                var accounts = tx.accounts();
                if (transfer.from().value() < transfer.to().value()) {
                    debit(accounts, transfer.from(), transfer.amount());
                    credit(accounts, transfer.to(), transfer.amount());
                } else {
                    credit(accounts, transfer.to(), transfer.amount());
                    debit(accounts, transfer.from(), transfer.amount());
                }
                return new TransferOutcome.Completed(tx.transfers().transfer(transfer));
            });
        } catch (Rejected e) {
            return new TransferOutcome.Rejected(e.rejection);
        }
    }

    private void debit(AccountRepository accounts, AccountId id, Money amount) {
        if (accounts.debitIfSufficient(id, amount)) return;
        throw new Rejected(accounts.exists(id)
                ? new TransferRejection.InsufficientFunds()
                : new TransferRejection.UnknownAccount(id));
    }

    private void credit(AccountRepository accounts, AccountId id, Money amount) {
        if (!accounts.credit(id, amount)) throw new Rejected(new TransferRejection.UnknownAccount(id));
    }

    private static final class Rejected extends RuntimeException {
        final TransferRejection rejection;
        Rejected(TransferRejection r) { super(null, null, false, false); this.rejection = r; }
    }
}

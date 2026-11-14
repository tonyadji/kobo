package com.lehnade.kobo.domain;

public interface AccountRepository {
    boolean debitIfSufficient(AccountId accountId, Money amount);
    boolean credit(AccountId accountId, Money amount);
    boolean exists(AccountId accountId);
}

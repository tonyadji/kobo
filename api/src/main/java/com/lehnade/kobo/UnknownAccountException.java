package com.lehnade.kobo;

public class UnknownAccountException extends KoboException {
    private long accountId;
    public UnknownAccountException(String message, long accountId) {
        super(message, APIError.UNKNOWN_ACCOUNT);
        this.accountId = accountId;
    }

    public long getAccountId() {
        return accountId;
    }
}

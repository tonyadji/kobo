package com.lehnade.kobo;

public class InsufficientFundException extends KoboException {
    public InsufficientFundException(String message) {
        super(message, APIError.INSUFFICIENT_FUNDS);
    }
}

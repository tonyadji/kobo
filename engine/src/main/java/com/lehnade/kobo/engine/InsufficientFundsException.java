package com.lehnade.kobo.engine;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() {
        super(null, null, false, false);
    }
}
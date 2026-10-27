package com.lehnade.kobo;

public class UnsufficientFundException extends RuntimeException {
    public UnsufficientFundException(String message) {
        super(message);
    }
}

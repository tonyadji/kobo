package com.lehnade.kobo;

public class IdempotencyKeyException extends KoboException {
    public IdempotencyKeyException(String message) {
        super(message, APIError.IDEMPOTENCY_KEY_ERROR);
    }
}

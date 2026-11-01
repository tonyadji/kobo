package com.lehnade.kobo;

public record IdempotencyKey(String key, String requestHash, String responseBody, String status, Integer httpStatus) {

    public static IdempotencyKey of(String key, String requestHash) {
        return new IdempotencyKey(key, requestHash, null, "PROCESSING", null);
    }
}

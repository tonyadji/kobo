package com.lehnade.kobo;

public record IdempotencyKey(String key, String requestHash, String outcome, Long transferId, Long accountId) {

    public static IdempotencyKey of(String key, String requestHash) {
        return new IdempotencyKey(key, requestHash, "PROCESSING", null, null);
    }
}

package com.lehnade.kobo.application;

import com.lehnade.kobo.IdempotencyKey;

import java.util.Optional;

public interface IdempotencyStore {
    boolean insert(IdempotencyKey key);
    void complete(IdempotencyKey key, TransferOutcome outcome);
    Optional<IdempotencyKey> find(String key);
}

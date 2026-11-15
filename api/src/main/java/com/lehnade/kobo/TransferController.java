package com.lehnade.kobo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lehnade.kobo.application.IdempotentTransfer;
import com.lehnade.kobo.application.TransferOutcome;
import com.lehnade.kobo.domain.AccountId;
import com.lehnade.kobo.domain.Money;
import com.lehnade.kobo.domain.Transfer;
import com.lehnade.kobo.domain.TransferRejection;
import io.javalin.http.Context;

import java.util.Map;
import java.util.Optional;

public class TransferController {

    private final IdempotentTransfer idempotentTransfer;

    public TransferController(IdempotentTransfer idempotentTransfer) {
        this.idempotentTransfer = idempotentTransfer;
    }

    public void transfer(Context ctx) throws JsonProcessingException {
        var key = requireIdempotencyKey(ctx);
        var requestDto =  ctx.bodyValidator(TransferRequestDto.class).get();
        var transferResponse = idempotentTransfer.execute(Transfer.between(
                new AccountId(requestDto.from()),
                new AccountId(requestDto.to()),
                Money.of(requestDto.amount())
                ),
                IdempotencyKey.of(key, HashHelper.hash(requestDto)));
        switch (transferResponse) {
            case TransferOutcome.Completed completed
                    -> ctx.status(201).json(new TransferResponseDto(completed.id().value()));
            case TransferOutcome.Processing processing -> ctx.status(409).json(payloadOf(APIError.IDEMPOTENCY_KEY_IN_PROGRESS));
            case TransferOutcome.KeyReused keyReused -> ctx.status(422).json(payloadOf(APIError.IDEMPOTENCY_KEY_ERROR));
            case TransferOutcome.Rejected rejected -> {
                switch (rejected.rejection()) {
                    case TransferRejection.InsufficientFunds insufficientFunds -> ctx.status(422).json(payloadOf(APIError.INSUFFICIENT_FUNDS));
                    case TransferRejection.UnknownAccount unknownAccount -> ctx.status(404).json(payloadOf(APIError.UNKNOWN_ACCOUNT)    );
                }
            }
        }
    }

    private String requireIdempotencyKey(Context ctx) {
        return Optional.ofNullable(ctx.header("Idempotency-Key"))
                .orElseThrow(() -> new InvalidTransferException("Idempotency-Key header is required"));
    }

    private String payloadOf(APIError e) throws JsonProcessingException {
        Map<String, Object> payload = Map.of("error", e);
        return new ObjectMapper().writeValueAsString(payload);
    }
}

package com.lehnade.kobo;

import io.javalin.http.Context;

import java.util.Optional;

public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    public void transfer(Context ctx) {
        var key = requireIdempotencyKey(ctx);
        var requestDto =  ctx.bodyValidator(TransferRequestDto.class).get();
        var transferResponse = transferService.transfer(
                requestDto.from(),
                requestDto.to(),
                requestDto.amount(),
                IdempotencyKey.of(key, HashHelper.hash(requestDto)));
        ctx.status(transferResponse.status()).json(transferResponse.body());
    }

    private String requireIdempotencyKey(Context ctx) {
        return Optional.ofNullable(ctx.header("Idempotency-Key"))
                .orElseThrow(() -> new InvalidTransferException("Idempotency-Key header is required"));
    }
}

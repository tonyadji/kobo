package com.lehnade.kobo;

import io.javalin.http.Context;

public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    public void transfer(Context ctx) {
        var requestDto =  ctx.bodyValidator(TransferRequestDto.class).get();
        var transferId = transferService.transfer(requestDto.from(), requestDto.to(), requestDto.amount());
        ctx.status(201).json(new TransferResponseDto(transferId));
    }
}

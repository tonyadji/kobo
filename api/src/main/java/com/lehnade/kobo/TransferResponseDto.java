package com.lehnade.kobo;

public record TransferResponseDto(long id) {

    public record StoredResponse(int status, String body) { }
}

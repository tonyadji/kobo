package com.lehnade.kobo;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferResponseDto(@JsonProperty("id")long id) {
}

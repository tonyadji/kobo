package com.lehnade.kobo.application;

public record TransferCommand(long from, long to, long amount) {}

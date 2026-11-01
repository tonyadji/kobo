package com.lehnade.kobo;

public class InvalidTransferException extends KoboException {
    public InvalidTransferException(String message) {
        super(message, APIError.BAD_REQUEST);
    }
}

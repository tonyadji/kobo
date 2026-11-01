package com.lehnade.kobo;

public class UnknownAccountException extends KoboException {
    public UnknownAccountException(String message) {
        super(message, APIError.UNKNOWN_ACCOUNT);
    }
}

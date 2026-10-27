package com.lehnade.kobo;

public class UnknownAccountException extends RuntimeException {
    public UnknownAccountException(String message) {
        super(message);
    }
}

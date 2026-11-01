package com.lehnade.kobo;

public class KoboException extends RuntimeException {
    protected APIError error;
    public KoboException(String message) {
        super(message);
    }

    public KoboException(String message, APIError error) {
        super(message);
        this.error = error;
    }

    public APIError getError() {
        return error;
    }
}

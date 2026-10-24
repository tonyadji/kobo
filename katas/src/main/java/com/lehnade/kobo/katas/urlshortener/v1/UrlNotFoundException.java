package com.lehnade.kobo.katas.urlshortener.v1;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String message) {
        super(message);
    }
}

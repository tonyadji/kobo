package com.lehnade.kobo.katas.urlshortener.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlShortenerTest {

    private String SHORT_URL_PREFIX = "https://kobo.short/";

    @Test
    void shortenShouldReturn7chars() {
        // arrange
        String longUrl = "https://www.kobo.lehnade.com";
        var urlShortener = new UrlShortener();

        // act
        String shortUrl = urlShortener.shorten(longUrl);

        // assert
        Assertions.assertEquals(7, shortUrl.length());
    }

    @Test
    void expandShouldReturnLongUrl() {
        // arrange
        String longUrl = "https://www.kobo.lehnade.com";
        var urlShortener = new UrlShortener();
        String shortCode = urlShortener.shorten(longUrl);

        // act
        String longUrlFromShortCode = urlShortener.expand(shortCode);

        // assert
        Assertions.assertEquals(longUrl, longUrlFromShortCode);
    }

    @Test
    void expandShouldThrowWhenCodeNotFound() {
        // arrange
        String longUrl = "https://www.kobo.lehnade.com";
        var urlShortener = new UrlShortener();
        String shortCode = urlShortener.shorten(longUrl);

        // act assert
        UrlNotFoundException exception = Assertions.assertThrows(UrlNotFoundException.class,
                () -> urlShortener.expand("1234567"));
        Assertions.assertEquals("Given code not found", exception.getMessage());
    }

    @Test
    void shortenShouldRejectInvalidUrl() {
        // arrange
        String invalidUrl = "httpswww.kobo.lehnade.com";
        var urlShortener = new UrlShortener();

        // act assert
        IllegalArgumentException invalidUrlException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> urlShortener.shorten(invalidUrl));
        Assertions.assertEquals("Invalid url", invalidUrlException.getMessage());
    }
 }

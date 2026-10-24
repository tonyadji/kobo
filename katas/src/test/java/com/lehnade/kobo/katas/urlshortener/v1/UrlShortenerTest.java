package com.lehnade.kobo.katas.urlshortener.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlShortenerTest {

    private String SHORT_URL_PREFIX = "https://kobo.short/";

    @Test
    void shortenShouldReturn7chars() {
        // arrange
        String longUrl = "https://www.google.com";
        var urlShortener = new UrlShortener();

        // act
        String shortUrl = urlShortener.shorten(longUrl);

        // assert
        Assertions.assertEquals(7, shortUrl.length());
    }
}

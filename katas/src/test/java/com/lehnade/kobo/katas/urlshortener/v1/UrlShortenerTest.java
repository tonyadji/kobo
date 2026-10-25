package com.lehnade.kobo.katas.urlshortener.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UrlShortenerTest {

    private final CodeGenerationStrategy base62Random = new Base62RandomGenerationStrategy();

    @Test
    void shortenShouldReturn7chars() {
        // arrange
        String longUrl = "https://www.kobo.lehnade.com";
        var urlShortener = new UrlShortener(base62Random);

        // act
        String shortUrl = urlShortener.shorten(longUrl);

        // assert
        Assertions.assertEquals(7, shortUrl.length());
    }

    @Test
    void expandShouldReturnLongUrl() {
        // arrange
        String longUrl = "https://www.kobo.lehnade.com";
        var urlShortener = new UrlShortener(base62Random);
        String shortCode = urlShortener.shorten(longUrl);

        // act
        String longUrlFromShortCode = urlShortener.expand(shortCode);

        // assert
        Assertions.assertEquals(longUrl, longUrlFromShortCode);
    }

    @Test
    void expandShouldThrowWhenCodeNotFound() {
        // arrange
        var urlShortener = new UrlShortener(base62Random);

        // act assert
        UrlNotFoundException exception = Assertions.assertThrows(UrlNotFoundException.class,
                () -> urlShortener.expand("1234567"));
        Assertions.assertEquals("Given code not found", exception.getMessage());
    }

    @Test
    void shortenShouldRejectInvalidUrl() {
        // arrange
        String invalidUrl = "httpswww.kobo.lehnade.com";
        var urlShortener = new UrlShortener(base62Random);

        // act assert
        IllegalArgumentException invalidUrlException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> urlShortener.shorten(invalidUrl));
        Assertions.assertEquals("Invalid url", invalidUrlException.getMessage());
    }

    @Test
    void twoDifferentUrlsNeverGetTheSameCode() {
        // arrange
        String[] codes = {"the-code-1","the-code-1","the-code-2"};
        var codeGenerator = new FakeCodeGenerator(codes);
        var urlShortener = new UrlShortener(codeGenerator);
        String longUrl1 = "https://www.kobo.lehnade.com";
        String longUrl2 = "https://www.kobo.lehnade.com/about";

        // act
        String shortCode1 = urlShortener.shorten(longUrl1);
        String shortCode2 = urlShortener.shorten(longUrl2);
        String expandedUrl1 = urlShortener.expand(shortCode1);

        // assert
        Assertions.assertNotEquals(shortCode1, shortCode2);
        Assertions.assertEquals(longUrl1, expandedUrl1);
        Assertions.assertEquals(3, codeGenerator.getCounter());
    }

    @Test
    void shortShouldRetryAtMostFiveTimesInCaseOfCollision () {
        // arrange
        String[] codes = {"the-code-1","the-code-1","the-code-1","the-code-1","the-code-1","the-code-1","the-code-2"};;
        var codeGenerator = new FakeCodeGenerator(codes);
        var urlShortener = new UrlShortener(codeGenerator);
        String longUrl1 = "https://www.kobo.lehnade.com";
        urlShortener.shorten(longUrl1);
        String longUrl2 = "https://www.kobo.lehnade.com/about";

        // act assert
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> urlShortener.shorten(longUrl2));
        Assertions.assertEquals("Could not generate unique code after 5 attempts.", exception.getMessage());
        Assertions.assertEquals(6, codeGenerator.getCounter());
    }

    @Test
    void shortShouldShortenWhenCollisionIsResolved () {
        // arrange
        String[] codes = {"the-code-1","the-code-1","the-code-1","the-code-1","the-code-1","the-code-2"};;
        var codeGenerator = new FakeCodeGenerator(codes);
        var urlShortener = new UrlShortener(codeGenerator);
        String longUrl1 = "https://www.kobo.lehnade.com";
        urlShortener.shorten(longUrl1);
        String longUrl2 = "https://www.kobo.lehnade.com/about";

        // act assert
        String shortCode2 = urlShortener.shorten(longUrl2);
        Assertions.assertNotEquals(shortCode2, "the-code-1");
        Assertions.assertEquals(shortCode2, "the-code-2");
        Assertions.assertEquals(longUrl2, urlShortener.expand(shortCode2));
        Assertions.assertEquals(longUrl1, urlShortener.expand("the-code-1"));
        Assertions.assertEquals(6, codeGenerator.getCounter());
    }

    @Test
    void shorteningTheSameUrlTwiceShouldReturnTheSameCode() {
        // arrange
        String[] codes = {"the-code-1","the-code-2","the-code-3","the-code-4","the-code-5","the-code-6"};;
        var codeGenerator = new FakeCodeGenerator(codes);
        var urlShortener = new UrlShortener(codeGenerator);
        String longUrl = "https://www.kobo.lehnade.com";
        String shortCode1 = urlShortener.shorten(longUrl);

        // act
        String shortCode2 = urlShortener.shorten(longUrl);

        // assert
        Assertions.assertEquals(shortCode1, shortCode2);
        Assertions.assertEquals(1, codeGenerator.getCounter());
    }

    static class FakeCodeGenerator implements CodeGenerationStrategy {
        private String[] codes;
        private int counter = 0;
        public FakeCodeGenerator(String... codes) {
            this.codes = codes;
        }

        @Override
        public String generate(int length) {
            if (counter >= codes.length) { throw new IllegalStateException("All codes used");}
            return codes[counter++];
        }

        public int getCounter() {
            return counter;
        }
    }
 }

package com.lehnade.kobo.katas.urlshortener.v1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
        String[] codes = {"the-code-1","the-code-1","the-code-1","the-code-1","the-code-1","the-code-1","the-code-2"};
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
        String[] codes = {"the-code-1","the-code-1","the-code-1","the-code-1","the-code-1","the-code-2"};
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
        String[] codes = {"the-code-1","the-code-2","the-code-3","the-code-4","the-code-5","the-code-6"};
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

    @RepeatedTest(10)
    void thousandThreadsShortenDifferentUrlEachGetsDifferentCodeAndTheOriginalUrlWhenExpand() throws InterruptedException {
        // arrange
        List<String> longUrls = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        IntStream.range(0, 1000).forEach(i -> {
            longUrls.add("https://www.kobo.lehnade.com/" + i);
            codes.add("code-" + i);
        });
        var codeGenerator = new FakeCodeGenerator(codes.toArray(new String[0]));
        var urlShortener = new UrlShortener(codeGenerator);
        var latch = new CountDownLatch(1);
        var threads = new Thread[1000];
        var success = new AtomicInteger(0);
        var counter = new AtomicInteger(0);
        var errors = new ConcurrentLinkedQueue<String>();

        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                try {
                    latch.await();
                    var count = counter.incrementAndGet() - 1;
                    var shortCode = urlShortener.shorten(longUrls.get(count));
                    var longUrl = urlShortener.expand(shortCode);
                    if (longUrl.equals(longUrls.get(count))) success.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }

            });
            threads[i] = thread;
        }

        // act
        for (Thread thread : threads) thread.start();
        latch.countDown();
        for (Thread thread : threads) thread.join();

        // assert
        Assertions.assertEquals(1000, success.get());
        Assertions.assertEquals(1000, codeGenerator.getCounter());
        Assertions.assertEquals(0, errors.size());
    }

    @RepeatedTest(10)
    void thousandThreadsShortenSameUrlGetTheCodeAndGeneratorCallOnce() throws InterruptedException {
        // arrange
        String originalUrl = "https://www.kobo.lehnade.com/";
        List<String> codes = new ArrayList<>();
        IntStream.range(0, 1000).forEach(i -> {
            codes.add("code-" + i);
        });
        var codeGenerator = new FakeCodeGenerator(codes.toArray(new String[0]));
        var urlShortener = new UrlShortener(codeGenerator);
        var latch = new CountDownLatch(1);
        var threads = new Thread[1000];
        var success = new AtomicInteger(0);
        var errors = new ConcurrentLinkedQueue<String>();

        for (int i = 0; i < 1000; i++) {
            Thread thread = new Thread(() -> {
                try {
                    latch.await();
                    var shortCode = urlShortener.shorten(originalUrl);
                    var longUrl = urlShortener.expand(shortCode);
                    if (longUrl.equals(originalUrl)) success.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (Exception e) {
                    errors.add(e.getMessage());
                }

            });
            threads[i] = thread;
        }

        // act
        for (Thread thread : threads) thread.start();
        latch.countDown();
        for (Thread thread : threads) thread.join();

        // assert
        Assertions.assertEquals(1000, success.get());
        Assertions.assertEquals(1, codeGenerator.getCounter());
        Assertions.assertEquals(0, errors.size());
    }

    static class FakeCodeGenerator implements CodeGenerationStrategy {
        private final String[] codes;
        private final AtomicInteger counter = new AtomicInteger(0);
        public FakeCodeGenerator(String... codes) {
            this.codes = codes;
        }

        @Override
        public String generate(int length) {
            int i = counter.incrementAndGet() - 1;
            if (i >= codes.length) { throw new IllegalStateException("All codes used");}

            return codes[i];
        }

        public int getCounter() {
            return counter.get();
        }
    }
 }

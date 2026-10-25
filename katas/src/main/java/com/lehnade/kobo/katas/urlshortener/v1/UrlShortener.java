package com.lehnade.kobo.katas.urlshortener.v1;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class UrlShortener {

    private static final int SHORT_URL_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 5;
    private static final String SHORT_URL_PREFIX = "https://kobo.short/";
    private static final Pattern VALID_URL_REGEX = Pattern.compile("^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$");

    private final ConcurrentHashMap<String, Urls> shortUrls;
    private final ConcurrentHashMap<String, String> urlToCode;
    private final CodeGenerationStrategy strategy;

    public UrlShortener(CodeGenerationStrategy strategy) {
        this.shortUrls = new ConcurrentHashMap<>();
        this.urlToCode = new ConcurrentHashMap<>();
        this.strategy = strategy;
    }

    public String shorten(String longUrl) {
        validateUrl(longUrl);
        if (urlToCode.containsKey(longUrl)) {
            return urlToCode.get(longUrl);
        }
        String code = "";
        int i;

        for (i = 0; i < MAX_ATTEMPTS; i++) {
            code = strategy.generate(SHORT_URL_LENGTH);
            Urls urls = shortUrls.computeIfAbsent(code,k -> {
                urlToCode.putIfAbsent(longUrl, k);
                return new Urls(SHORT_URL_PREFIX+k, longUrl);
            });
            if (urls.longUrl().equals(longUrl)) {
                break;
            }
        }
        if (i == MAX_ATTEMPTS) {
            throw new IllegalStateException("Could not generate unique code after "+MAX_ATTEMPTS+" attempts.");
        }

        return code;
    }

    public String expand(String code) {
        Urls urls = shortUrls.get(code);
        if (urls != null) {
            return urls.longUrl();
        }
        throw new UrlNotFoundException("Given code not found");
    }

    private void validateUrl(String longUrl) {
        if (longUrl == null || longUrl.isEmpty()) {
            throw new IllegalArgumentException("Invalid url");
        }
        if (VALID_URL_REGEX.matcher(longUrl).matches()) {
            return;
        }
        throw new IllegalArgumentException("Invalid url");
    }

    record Urls(String shortUrl, String longUrl) {}
}

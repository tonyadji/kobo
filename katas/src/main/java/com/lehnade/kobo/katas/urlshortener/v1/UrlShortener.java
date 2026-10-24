package com.lehnade.kobo.katas.urlshortener.v1;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class UrlShortener {

    private static final int SHORT_URL_LENGTH = 7;
    private static final String BASE_62ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String SHORT_URL_PREFIX = "https://kobo.short/";
    private static final Pattern VALID_URL_REGEX = Pattern.compile("^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$");

    private final ConcurrentHashMap<String, Urls> shortUrls;

    public UrlShortener() {
        this.shortUrls = new ConcurrentHashMap<>();
    }

    public String shorten(String longUrl) {
        validateUrl(longUrl);
        String code = generateBase62Id(SHORT_URL_LENGTH);
        while (shortUrls.containsKey(code)) {
            code = generateBase62Id(SHORT_URL_LENGTH);
        }
        var shortUrl = SHORT_URL_PREFIX.concat(code);
        shortUrls.putIfAbsent(code, new Urls(shortUrl, longUrl));
        return code;
    }

    public String expand(String code) {
        if (shortUrls.containsKey(code)) {
            return shortUrls.get(code).longUrl();
        }
        throw new UrlNotFoundException("Given code not found");
    }

    private String generateBase62Id(int length) {
        var code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(BASE_62ALPHABET.charAt(new Random().nextInt(BASE_62ALPHABET.length())));
        }
        return code.toString();
    }

    private void validateUrl(String longUrl) {
        if (VALID_URL_REGEX.matcher(longUrl).matches()) {
            return;
        }
        throw new IllegalArgumentException("Invalid url");
    }

    record Urls(String shortUrl, String longUrl) {}
}

package com.lehnade.kobo.katas.urlshortener.v1;

import java.security.SecureRandom;
import java.util.Random;

public class Base62RandomGenerationStrategy implements CodeGenerationStrategy {
    private final Random random = new SecureRandom();
    private static final String BASE_62ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @Override
    public String generate(int length) {
        var code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(BASE_62ALPHABET.charAt(random.nextInt(BASE_62ALPHABET.length())));
        }
        return code.toString();
    }
}

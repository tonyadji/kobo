package com.lehnade.kobo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public class HashHelper {
    private static final ObjectMapper mapper = new ObjectMapper();

    private HashHelper() {}

    public static String hash (Object raw) {
        Map<String, Object> map = mapper.convertValue(raw, Map.class);
        String objectAsString = null;
        try {
            objectAsString = mapper.writeValueAsString(map);
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(messageDigest.digest(objectAsString.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

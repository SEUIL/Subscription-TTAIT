package com.ttait.subscription.external.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;

@Component
public class CanonicalJsonHasher {

    private final ObjectMapper objectMapper;

    public CanonicalJsonHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String hash(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        try {
            byte[] canonicalJson = objectMapper.writeValueAsBytes(canonicalize(node));
            return sha256(canonicalJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to canonicalize JSON", e);
        }
    }

    public String hashJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return hash(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON for hashing", e);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode canonical = objectMapper.createObjectNode();
            Iterator<String> fieldNames = node.fieldNames();
            StreamSupport.stream(((Iterable<String>) () -> fieldNames).spliterator(), false)
                    .sorted()
                    .forEach(fieldName -> canonical.set(fieldName, canonicalize(node.get(fieldName))));
            return canonical;
        }
        if (node.isArray()) {
            ArrayNode canonical = objectMapper.createArrayNode();
            node.forEach(item -> canonical.add(canonicalize(item)));
            return canonical;
        }
        return node.deepCopy();
    }

    private String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash canonical JSON", e);
        }
    }

    public String sha256Text(String value) {
        if (value == null) {
            return null;
        }
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }
}

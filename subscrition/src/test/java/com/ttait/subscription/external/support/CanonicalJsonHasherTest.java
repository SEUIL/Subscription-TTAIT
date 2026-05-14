package com.ttait.subscription.external.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CanonicalJsonHasherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CanonicalJsonHasher hasher = new CanonicalJsonHasher(objectMapper);

    @Test
    void hashesObjectJsonByCanonicalKeyOrder() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {"b":2,"a":{"y":2,"x":1},"items":[{"z":3,"a":1}]}
                """);
        JsonNode second = objectMapper.readTree("""
                {"items":[{"a":1,"z":3}],"a":{"x":1,"y":2},"b":2}
                """);

        assertThat(hasher.hash(first)).isEqualTo(hasher.hash(second));
    }

    @Test
    void keepsArrayOrderSignificant() throws Exception {
        JsonNode first = objectMapper.readTree("""
                {"items":[1,2]}
                """);
        JsonNode second = objectMapper.readTree("""
                {"items":[2,1]}
                """);

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    @Test
    void hashesJsonTextCanonically() {
        String first = """
                {"b":2,"a":1}
                """;
        String second = """
                {"a":1,"b":2}
                """;

        assertThat(hasher.hashJson(first)).isEqualTo(hasher.hashJson(second));
    }
}

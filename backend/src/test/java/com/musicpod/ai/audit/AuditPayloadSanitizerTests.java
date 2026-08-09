package com.musicpod.ai.audit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class AuditPayloadSanitizerTests {

    private AuditPayloadSanitizer sanitizer;

    @BeforeEach
    void setUp() {

        JsonMapper jsonMapper =
                JsonMapper.builder()
                        .build();

        sanitizer =
                new AuditPayloadSanitizer(
                        jsonMapper
                );
    }

    @Test
    void redactsSensitiveJsonFields() {

        String input = """
                {
                  "name": "Sneha",
                  "password": "musicpod123!",
                  "accessToken": "abc123",
                  "nested": {
                    "authorization": "Bearer secret-token",
                    "apiKey": "sk-super-secret"
                  }
                }
                """;

        String result =
                sanitizer.toolPayload(
                        input
                );

        assertTrue(
                result.contains(
                        "\"password\":\"[REDACTED]\""
                )
        );

        assertTrue(
                result.contains(
                        "\"accessToken\":\"[REDACTED]\""
                )
        );

        assertTrue(
                result.contains(
                        "\"authorization\":\"[REDACTED]\""
                )
        );

        assertTrue(
                result.contains(
                        "\"apiKey\":\"[REDACTED]\""
                )
        );

        assertFalse(
                result.contains(
                        "musicpod123!"
                )
        );

        assertFalse(
                result.contains(
                        "abc123"
                )
        );

        assertFalse(
                result.contains(
                        "sk-super-secret"
                )
        );
    }

    @Test
    void truncatesLargeToolPayloads() {

        String input =
                "x".repeat(
                        20_000
                );

        String result =
                sanitizer.toolPayload(
                        input
                );

        assertTrue(
                result.endsWith(
                        "...[TRUNCATED]"
                )
        );

        /*
         * 16,000 chars +
         * truncation marker.
         */
        assertTrue(
                result.length()
                        < 16_100
        );
    }

    @Test
    void redactsSensitivePlainText() {

        String input =
                """
                authorization=Bearer abc.def.ghi
                password=supersecret
                key=sk-example-secret-key
                """;

        String result =
                sanitizer.error(
                        input
                );

        assertFalse(
                result.contains(
                        "abc.def.ghi"
                )
        );

        assertFalse(
                result.contains(
                        "supersecret"
                )
        );

        assertFalse(
                result.contains(
                        "sk-example-secret-key"
                )
        );
    }
}
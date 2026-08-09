package com.musicpod.ai.audit;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
public class AuditPayloadSanitizer {

    private static final int MAX_TOOL_PAYLOAD_LENGTH =
            16_000;

    private static final int MAX_RESPONSE_LENGTH =
            32_000;

    private static final int MAX_ERROR_LENGTH =
            4_000;

    private static final Set<String> SENSITIVE_KEYS =
            Set.of(
                    "password",
                    "passwd",
                    "secret",
                    "token",
                    "accesstoken",
                    "refreshtoken",
                    "authorization",
                    "apikey"
            );

    private final JsonMapper jsonMapper;

    public AuditPayloadSanitizer(
            JsonMapper jsonMapper) {

        this.jsonMapper =
                jsonMapper;
    }

    public String toolPayload(
            String value) {

        if (value == null) {
            return null;
        }

        String sanitized =
                sanitizeJsonIfPossible(
                        value
                );

        return truncate(
                sanitized,
                MAX_TOOL_PAYLOAD_LENGTH
        );
    }

    public String response(
            String value) {

        return truncate(
                sanitizeText(value),
                MAX_RESPONSE_LENGTH
        );
    }

    public String error(
            String value) {

        return truncate(
                sanitizeText(value),
                MAX_ERROR_LENGTH
        );
    }

    private String sanitizeJsonIfPossible(
            String value) {

        try {

            JsonNode root =
                    jsonMapper.readTree(
                            value
                    );

            redact(
                    root
            );

            return jsonMapper
                    .writeValueAsString(
                            root
                    );

        } catch (Exception exception) {

            /*
             * Tool input/output is not guaranteed
             * to always be valid JSON.
             *
             * Fall back to text sanitization.
             */
            return sanitizeText(
                    value
            );
        }
    }

    private void redact(
            JsonNode node) {

        if (node instanceof ObjectNode objectNode) {

            /*
             * Jackson 3 removed ObjectNode.fields().
             *
             * Copy the property names before modifying
             * values in the ObjectNode.
             */
            for (String fieldName :
                    new ArrayList<>(
                            objectNode.propertyNames()
                    )) {

                JsonNode value =
                        objectNode.get(
                                fieldName
                        );

                if (isSensitive(
                        fieldName
                )) {

                    objectNode.put(
                            fieldName,
                            "[REDACTED]"
                    );

                } else if (value != null) {

                    redact(
                            value
                    );
                }
            }

            return;
        }

        if (node instanceof ArrayNode arrayNode) {

            for (JsonNode child :
                    arrayNode) {

                redact(
                        child
                );
            }
        }
    }

    private boolean isSensitive(
            String key) {

        String normalized =
                key
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace("_", "")
                        .replace("-", "");

        return SENSITIVE_KEYS
                .contains(
                        normalized
                );
    }

    private String sanitizeText(
            String value) {

        if (value == null) {
            return null;
        }

        String sanitized =
                value.replaceAll(
                        "(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+",
                        "$1[REDACTED]"
                );

        sanitized =
                sanitized.replaceAll(
                        "sk-[A-Za-z0-9_-]+",
                        "[REDACTED_OPENAI_KEY]"
                );

        sanitized =
                sanitized.replaceAll(
                        "(?i)(password\\s*[:=]\\s*)[^\\s,;]+",
                        "$1[REDACTED]"
                );

        return sanitized;
    }

    private String truncate(
            String value,
            int maxLength) {

        if (value == null
                || value.length() <= maxLength) {

            return value;
        }

        return value.substring(
                0,
                maxLength
        ) + "...[TRUNCATED]";
    }
}
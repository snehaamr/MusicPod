package com.musicpod.messaging.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String topic,
        String messageKey,
        String payload,
        int attempts,
        Instant createdAt
) {
}
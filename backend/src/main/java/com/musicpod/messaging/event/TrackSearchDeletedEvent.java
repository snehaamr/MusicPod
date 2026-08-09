package com.musicpod.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record TrackSearchDeletedEvent(
        UUID eventId,
        UUID trackId,
        Instant occurredAt,
        int schemaVersion
) {

    public static final String EVENT_TYPE =
            "track.search.deleted.v1";

    private static final int CURRENT_SCHEMA_VERSION =
            1;

    public static TrackSearchDeletedEvent from(
            UUID trackId) {

        return new TrackSearchDeletedEvent(
                UUID.randomUUID(),
                trackId,
                Instant.now(),
                CURRENT_SCHEMA_VERSION
        );
    }
}
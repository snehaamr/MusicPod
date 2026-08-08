package com.musicpod.messaging.event;

import java.time.Instant;
import java.util.UUID;

import com.musicpod.playback.PlaybackEvent;

public record PlaybackRecordedEvent(
        UUID eventId,
        UUID userId,
        UUID trackId,
        int playedMs,
        Instant playedAt,
        int schemaVersion
) {

    public static final String EVENT_TYPE =
            "playback.recorded.v1";

    private static final int CURRENT_SCHEMA_VERSION =
            1;

    public static PlaybackRecordedEvent from(
            PlaybackEvent playbackEvent) {

        return new PlaybackRecordedEvent(
                playbackEvent.getId(),
                playbackEvent.getUser().getId(),
                playbackEvent.getTrack().getId(),
                playbackEvent.getPlayedMs(),
                playbackEvent.getPlayedAt(),
                CURRENT_SCHEMA_VERSION
        );
    }
}
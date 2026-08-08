package com.musicpod.catalog.track;

import java.time.Instant;
import java.util.UUID;

public record TrackResponse(
        UUID id,
        UUID albumId,
        String title,
        int trackNumber,
        int durationMs,
        boolean explicit,
        Instant createdAt,
        Instant updatedAt
) {

    public static TrackResponse from(Track track) {

        return new TrackResponse(
                track.getId(),
                track.getAlbum().getId(),
                track.getTitle(),
                track.getTrackNumber(),
                track.getDurationMs(),
                track.isExplicit(),
                track.getCreatedAt(),
                track.getUpdatedAt()
        );
    }
}
package com.musicpod.messaging.event;

import java.time.Instant;
import java.util.UUID;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.track.Track;

public record TrackSearchUpsertedEvent(
        UUID eventId,
        UUID trackId,
        String title,
        UUID albumId,
        String albumTitle,
        UUID artistId,
        String artistName,
        int durationMs,
        boolean explicit,
        Instant occurredAt,
        int schemaVersion
) {

    public static final String EVENT_TYPE =
            "track.search.upserted.v1";

    private static final int CURRENT_SCHEMA_VERSION =
            1;

    public static TrackSearchUpsertedEvent from(
            Track track) {

        Album album =
                track.getAlbum();

        Artist artist =
                album.getArtist();

        return new TrackSearchUpsertedEvent(
                UUID.randomUUID(),
                track.getId(),
                track.getTitle(),
                album.getId(),
                album.getTitle(),
                artist.getId(),
                artist.getName(),
                track.getDurationMs(),
                track.isExplicit(),
                Instant.now(),
                CURRENT_SCHEMA_VERSION
        );
    }
}
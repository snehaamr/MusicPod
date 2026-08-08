package com.musicpod.library.playlist;

import java.time.Instant;
import java.util.UUID;

public record PlaylistResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static PlaylistResponse from(
            Playlist playlist) {

        return new PlaylistResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getCreatedAt(),
                playlist.getUpdatedAt()
        );
    }
}
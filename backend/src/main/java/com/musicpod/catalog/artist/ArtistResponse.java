package com.musicpod.catalog.artist;

import java.time.Instant;
import java.util.UUID;

public record ArtistResponse(
        UUID id,
        String name,
        String bio,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(
                artist.getId(),
                artist.getName(),
                artist.getBio(),
                artist.getImageUrl(),
                artist.getCreatedAt(),
                artist.getUpdatedAt()
        );
    }
}
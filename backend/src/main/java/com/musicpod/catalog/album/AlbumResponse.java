package com.musicpod.catalog.album;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlbumResponse(
        UUID id,
        UUID artistId,
        String title,
        LocalDate releaseDate,
        String coverImageUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static AlbumResponse from(Album album) {

        return new AlbumResponse(
                album.getId(),
                album.getArtist().getId(),
                album.getTitle(),
                album.getReleaseDate(),
                album.getCoverImageUrl(),
                album.getCreatedAt(),
                album.getUpdatedAt()
        );
    }
}
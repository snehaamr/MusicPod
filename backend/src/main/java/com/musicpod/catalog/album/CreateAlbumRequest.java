package com.musicpod.catalog.album;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAlbumRequest(

        @NotBlank(message = "Album title is required")
        @Size(
                max = 300,
                message = "Album title must not exceed 300 characters"
        )
        String title,

        LocalDate releaseDate,

        @Size(
                max = 1000,
                message = "Cover image URL must not exceed 1000 characters"
        )
        String coverImageUrl

) {
}
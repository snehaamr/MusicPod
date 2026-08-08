package com.musicpod.catalog.artist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateArtistRequest(

        @NotBlank(message = "Artist name is required")
        @Size(
                max = 200,
                message = "Artist name must not exceed 200 characters"
        )
        String name,

        String bio,

        @Size(
                max = 1000,
                message = "Image URL must not exceed 1000 characters"
        )
        String imageUrl

) {
}
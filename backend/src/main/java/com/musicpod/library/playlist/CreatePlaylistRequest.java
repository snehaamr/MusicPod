package com.musicpod.library.playlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlaylistRequest(

        @NotBlank(
                message = "Playlist name is required"
        )
        @Size(
                max = 200,
                message = "Playlist name must not exceed 200 characters"
        )
        String name,

        @Size(
                max = 1000,
                message = "Playlist description must not exceed 1000 characters"
        )
        String description

) {

    public CreatePlaylistRequest {

        if (name != null) {
            name = name.trim();
        }

        if (description != null) {
            description = description.trim();
        }
    }
}
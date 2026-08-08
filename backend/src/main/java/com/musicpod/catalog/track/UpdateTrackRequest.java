package com.musicpod.catalog.track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateTrackRequest(

        @NotBlank(message = "Track title is required")
        @Size(
                max = 300,
                message = "Track title must not exceed 300 characters"
        )
        String title,

        @Positive(message = "Track number must be greater than 0")
        int trackNumber,

        @Positive(message = "Duration must be greater than 0")
        int durationMs,

        boolean explicit

) {
}
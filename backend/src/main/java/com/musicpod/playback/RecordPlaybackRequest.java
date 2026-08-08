package com.musicpod.playback;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordPlaybackRequest(

        @NotNull(
                message = "Track ID is required"
        )
        UUID trackId,

        @Positive(
                message = "Played milliseconds must be greater than 0"
        )
        int playedMs

) {
}
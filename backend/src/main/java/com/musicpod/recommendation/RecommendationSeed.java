package com.musicpod.recommendation;

import java.time.Instant;
import java.util.UUID;

public record RecommendationSeed(

        UUID trackId,

        String title,

        UUID albumId,

        String albumTitle,

        UUID artistId,

        String artistName,

        boolean liked,

        long recentPlayCount,

        Instant lastPlayedAt,

        long signalScore

) {
}
package com.musicpod.recommendation;

import com.musicpod.catalog.track.TrackResponse;

public record RecommendationResponse(

        int rank,

        TrackResponse track,

        long score,

        String reason

) {
}
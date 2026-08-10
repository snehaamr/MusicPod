package com.musicpod.recommendation;

import java.util.UUID;

public record RecommendationCandidate(

        UUID trackId,

        long score,

        String reason

) {
}
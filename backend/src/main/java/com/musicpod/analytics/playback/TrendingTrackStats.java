package com.musicpod.analytics.playback;

import java.util.UUID;

public record TrendingTrackStats(
        UUID trackId,
        long currentHourPlays,
        long playsLast24Hours,
        long playsLast7Days,
        long playedMsLast7Days,
        long trendingScore
) {
}
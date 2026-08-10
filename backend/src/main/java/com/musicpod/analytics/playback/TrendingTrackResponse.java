package com.musicpod.analytics.playback;

import com.musicpod.catalog.track.TrackResponse;

public record TrendingTrackResponse(

        int rank,

        TrackResponse track,

        long currentHourPlays,

        long playsLast24Hours,

        long playsLast7Days,

        long playedMsLast7Days,

        long trendingScore

) {
}
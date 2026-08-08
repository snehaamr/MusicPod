package com.musicpod.analytics.playback;

import java.time.Instant;
import java.util.UUID;

public record TrackPlayStats(
        UUID trackId,
        long playCount,
        long totalPlayedMs,
        Instant lastPlayedAt
) {
}
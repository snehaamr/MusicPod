package com.musicpod.ai.curator;

public record PlaylistDurationResult(
        int trackCount,
        long totalDurationMs,
        String formattedDuration
) {
}
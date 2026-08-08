package com.musicpod.playback;

import java.time.Instant;
import java.util.UUID;

import com.musicpod.catalog.track.TrackResponse;

public record PlaybackEventResponse(
        UUID id,
        TrackResponse track,
        int playedMs,
        Instant playedAt
) {

    public static PlaybackEventResponse from(
            PlaybackEvent event) {

        return new PlaybackEventResponse(
                event.getId(),
                TrackResponse.from(
                        event.getTrack()
                ),
                event.getPlayedMs(),
                event.getPlayedAt()
        );
    }
}
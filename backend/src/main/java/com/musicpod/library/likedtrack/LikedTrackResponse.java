package com.musicpod.library.likedtrack;

import java.time.Instant;

import com.musicpod.catalog.track.TrackResponse;

public record LikedTrackResponse(
        TrackResponse track,
        Instant likedAt
) {

    public static LikedTrackResponse from(
            LikedTrack likedTrack) {

        return new LikedTrackResponse(
                TrackResponse.from(
                        likedTrack.getTrack()
                ),
                likedTrack.getLikedAt()
        );
    }
}
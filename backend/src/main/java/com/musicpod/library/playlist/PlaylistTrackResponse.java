package com.musicpod.library.playlist;

import java.time.Instant;

import com.musicpod.catalog.track.TrackResponse;

public record PlaylistTrackResponse(
        int position,
        TrackResponse track,
        Instant addedAt
) {

    public static PlaylistTrackResponse from(
            PlaylistTrack playlistTrack) {

        return new PlaylistTrackResponse(
                playlistTrack.getPosition(),
                TrackResponse.from(
                        playlistTrack.getTrack()
                ),
                playlistTrack.getAddedAt()
        );
    }
}
package com.musicpod.search.track;

import java.util.UUID;

public record TrackSearchResult(
        UUID trackId,
        String title,
        UUID albumId,
        String albumTitle,
        UUID artistId,
        String artistName,
        int durationMs,
        boolean explicit,
        double score
) {

    public static TrackSearchResult from(
            TrackSearchDocument document,
            double score) {

        return new TrackSearchResult(
                document.trackId(),
                document.title(),
                document.albumId(),
                document.albumTitle(),
                document.artistId(),
                document.artistName(),
                document.durationMs(),
                document.explicit(),
                score
        );
    }
}
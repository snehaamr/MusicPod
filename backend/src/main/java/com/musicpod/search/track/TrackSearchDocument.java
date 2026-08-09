package com.musicpod.search.track;

import java.util.List;
import java.util.UUID;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.track.Track;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;

public record TrackSearchDocument(
        UUID trackId,
        String title,
        UUID albumId,
        String albumTitle,
        UUID artistId,
        String artistName,
        int durationMs,
        boolean explicit,
        String semanticText,
        List<Float> embedding
) {

    public static TrackSearchDocument from(
            Track track,
            String semanticText,
            List<Float> embedding) {

        Album album =
                track.getAlbum();

        Artist artist =
                album.getArtist();

        return new TrackSearchDocument(
                track.getId(),
                track.getTitle(),
                album.getId(),
                album.getTitle(),
                artist.getId(),
                artist.getName(),
                track.getDurationMs(),
                track.isExplicit(),
                semanticText,
                embedding
        );
    }

    public static TrackSearchDocument from(
            TrackSearchUpsertedEvent event,
            String semanticText,
            List<Float> embedding) {

        return new TrackSearchDocument(
                event.trackId(),
                event.title(),
                event.albumId(),
                event.albumTitle(),
                event.artistId(),
                event.artistName(),
                event.durationMs(),
                event.explicit(),
                semanticText,
                embedding
        );
    }
}
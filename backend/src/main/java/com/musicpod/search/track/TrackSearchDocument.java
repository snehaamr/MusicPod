package com.musicpod.search.track;

import java.util.UUID;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.track.Track;

public record TrackSearchDocument(
        UUID trackId,
        String title,
        UUID albumId,
        String albumTitle,
        UUID artistId,
        String artistName,
        int durationMs,
        boolean explicit
) {

    public static TrackSearchDocument from(
            Track track) {

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
                track.isExplicit()
        );
    }
}
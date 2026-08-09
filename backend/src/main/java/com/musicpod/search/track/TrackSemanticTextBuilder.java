package com.musicpod.search.track;

import org.springframework.stereotype.Component;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.track.Track;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;

@Component
public class TrackSemanticTextBuilder {

    public String build(
            Track track) {

        Album album =
                track.getAlbum();

        Artist artist =
                album.getArtist();

        return build(
                track.getTitle(),
                artist.getName(),
                album.getTitle()
        );
    }

    public String build(
            TrackSearchUpsertedEvent event) {

        return build(
                event.title(),
                event.artistName(),
                event.albumTitle()
        );
    }

    private String build(
            String title,
            String artistName,
            String albumTitle) {

        return """
                Track: %s
                Artist: %s
                Album: %s
                """.formatted(
                title,
                artistName,
                albumTitle
        );
    }
}
package com.musicpod.search.track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.track.Track;

class TrackSemanticTextBuilderTests {

    @Test
    void buildsStableSemanticText() {

        Track track =
                org.mockito.Mockito.mock(
                        Track.class
                );

        Album album =
                org.mockito.Mockito.mock(
                        Album.class
                );

        Artist artist =
                org.mockito.Mockito.mock(
                        Artist.class
                );

        when(
                track.getAlbum()
        ).thenReturn(
                album
        );

        when(
                album.getArtist()
        ).thenReturn(
                artist
        );

        when(
                track.getTitle()
        ).thenReturn(
                "Clocks"
        );

        when(
                artist.getName()
        ).thenReturn(
                "Coldplay"
        );

        when(
                album.getTitle()
        ).thenReturn(
                "A Rush of Blood to the Head"
        );

        TrackSemanticTextBuilder builder =
                new TrackSemanticTextBuilder();

        String result =
                builder.build(
                        track
                );

        String expected = """
                Track: Clocks
                Artist: Coldplay
                Album: A Rush of Blood to the Head
                """;

        assertEquals(
                expected,
                result
        );
    }
}
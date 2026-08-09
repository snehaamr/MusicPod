package com.musicpod.ai.curator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.auth.CurrentUserProvider;
import com.musicpod.library.playlist.CreatePlaylistRequest;
import com.musicpod.library.playlist.PlaylistResponse;
import com.musicpod.library.playlist.PlaylistService;
import com.musicpod.library.playlist.PlaylistTrackResponse;

@ExtendWith(MockitoExtension.class)
class MusicCuratorWriteToolsTests {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private PlaylistService playlistService;

    private MusicCuratorWriteTools tools;

    @BeforeEach
    void setUp() {

        tools =
                new MusicCuratorWriteTools(
                        currentUserProvider,
                        playlistService
                );
    }

    @Test
    void createPlaylistUsesAuthenticatedUserId() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        PlaylistResponse expected =
                new PlaylistResponse(
                        UUID.randomUUID(),
                        "AI Evening Mix",
                        "Created by curator",
                        Instant.now(),
                        Instant.now()
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                playlistService.create(
                        authenticatedUserId,
                        new CreatePlaylistRequest(
                                "AI Evening Mix",
                                "Created by curator"
                        )
                )
        ).thenReturn(
                expected
        );

        PlaylistResponse result =
                tools.createPlaylist(
                        "AI Evening Mix",
                        "Created by curator"
                );

        assertSame(
                expected,
                result
        );

        verify(
                currentUserProvider
        ).userId();

        verify(
                playlistService
        ).create(
                authenticatedUserId,
                new CreatePlaylistRequest(
                        "AI Evening Mix",
                        "Created by curator"
                )
        );
    }

    @Test
    void addTracksUsesAuthenticatedUserId() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        UUID playlistId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        PlaylistTrackResponse expected =
                org.mockito.Mockito.mock(
                        PlaylistTrackResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                playlistService.addTrack(
                        authenticatedUserId,
                        playlistId,
                        trackId
                )
        ).thenReturn(
                expected
        );

        List<PlaylistTrackResponse> result =
                tools.addTracksToPlaylist(
                        playlistId.toString(),
                        List.of(
                                trackId.toString()
                        )
                );

        assertEquals(
                1,
                result.size()
        );

        assertSame(
                expected,
                result.get(0)
        );

        verify(
                playlistService
        ).addTrack(
                authenticatedUserId,
                playlistId,
                trackId
        );
    }

    @Test
    void rejectsEmptyTrackList() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.addTracksToPlaylist(
                                UUID.randomUUID()
                                        .toString(),
                                List.of()
                        )
        );
    }

    @Test
    void rejectsNullTrackList() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.addTracksToPlaylist(
                                UUID.randomUUID()
                                        .toString(),
                                null
                        )
        );
    }

    @Test
    void rejectsMoreThanTwentyFiveTracks() {

        List<String> trackIds =
                java.util.stream.IntStream
                        .range(
                                0,
                                26
                        )
                        .mapToObj(
                                ignored ->
                                        UUID.randomUUID()
                                                .toString()
                        )
                        .toList();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.addTracksToPlaylist(
                                UUID.randomUUID()
                                        .toString(),
                                trackIds
                        )
        );
    }

    @Test
    void addsTracksInRequestedOrder() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        UUID playlistId =
                UUID.randomUUID();

        UUID firstTrackId =
                UUID.randomUUID();

        UUID secondTrackId =
                UUID.randomUUID();

        PlaylistTrackResponse firstResponse =
                org.mockito.Mockito.mock(
                        PlaylistTrackResponse.class
                );

        PlaylistTrackResponse secondResponse =
                org.mockito.Mockito.mock(
                        PlaylistTrackResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                playlistService.addTrack(
                        authenticatedUserId,
                        playlistId,
                        firstTrackId
                )
        ).thenReturn(
                firstResponse
        );

        when(
                playlistService.addTrack(
                        authenticatedUserId,
                        playlistId,
                        secondTrackId
                )
        ).thenReturn(
                secondResponse
        );

        List<PlaylistTrackResponse> result =
                tools.addTracksToPlaylist(
                        playlistId.toString(),
                        List.of(
                                firstTrackId.toString(),
                                secondTrackId.toString()
                        )
                );

        assertEquals(
                2,
                result.size()
        );

        assertSame(
                firstResponse,
                result.get(0)
        );

        assertSame(
                secondResponse,
                result.get(1)
        );
    }
}
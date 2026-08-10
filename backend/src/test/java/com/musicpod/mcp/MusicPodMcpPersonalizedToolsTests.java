package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.common.api.PageResponse;
import com.musicpod.library.likedtrack.LikedTrackResponse;
import com.musicpod.library.likedtrack.LikedTrackService;
import com.musicpod.playback.PlaybackEventResponse;
import com.musicpod.playback.PlaybackService;

import com.musicpod.auth.CurrentUserProvider;

@ExtendWith(MockitoExtension.class)
class MusicPodMcpPersonalizedToolsTests {

    @Mock
    private CurrentUserProvider
            currentUserProvider;

    @Mock
    private LikedTrackService
            likedTrackService;

    @Mock
    private PlaybackService
            playbackService;

    private MusicPodMcpPersonalizedTools
            tools;

    @BeforeEach
    void setUp() {

        tools =
                new MusicPodMcpPersonalizedTools(
                        currentUserProvider,
                        likedTrackService,
                        playbackService
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    void likedTracksUsesAuthenticatedUser() {

        UUID userId =
                UUID.randomUUID();

        PageResponse<LikedTrackResponse> response =
                org.mockito.Mockito.mock(
                        PageResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                userId
        );

        when(
                likedTrackService.getLikedTracks(
                        userId,
                        0,
                        10
                )
        ).thenReturn(
                response
        );

        PageResponse<LikedTrackResponse> result =
                tools.getLikedTracks(
                        null,
                        null
                );

        assertSame(
                response,
                result
        );

        verify(
                currentUserProvider
        ).userId();

        verify(
                likedTrackService
        ).getLikedTracks(
                userId,
                0,
                10
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void recentlyPlayedUsesAuthenticatedUser() {

        UUID userId =
                UUID.randomUUID();

        PageResponse<PlaybackEventResponse> response =
                org.mockito.Mockito.mock(
                        PageResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                userId
        );

        when(
                playbackService.getRecentlyPlayed(
                        userId,
                        2,
                        5
                )
        ).thenReturn(
                response
        );

        PageResponse<PlaybackEventResponse> result =
                tools.getRecentlyPlayed(
                        2,
                        5
                );

        assertSame(
                response,
                result
        );

        verify(
                currentUserProvider
        ).userId();

        verify(
                playbackService
        ).getRecentlyPlayed(
                userId,
                2,
                5
        );
    }

    @Test
    void rejectsNegativePageBeforeResolvingUser() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getLikedTracks(
                                -1,
                                10
                        )
        );

        verifyNoInteractions(
                currentUserProvider,
                likedTrackService,
                playbackService
        );
    }

    @Test
    void rejectsSizeAboveMaximumBeforeResolvingUser() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getRecentlyPlayed(
                                0,
                                21
                        )
        );

        verifyNoInteractions(
                currentUserProvider,
                likedTrackService,
                playbackService
        );
    }
}
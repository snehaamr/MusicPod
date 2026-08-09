package com.musicpod.ai.curator;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.auth.CurrentUserProvider;
import com.musicpod.common.api.PageResponse;
import com.musicpod.library.likedtrack.LikedTrackResponse;
import com.musicpod.library.likedtrack.LikedTrackService;
import com.musicpod.playback.PlaybackEventResponse;
import com.musicpod.playback.PlaybackService;

@ExtendWith(MockitoExtension.class)
class PersonalizedCuratorToolsTests {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private LikedTrackService likedTrackService;

    @Mock
    private PlaybackService playbackService;

    private PersonalizedCuratorTools tools;

    @BeforeEach
    void setUp() {

        tools =
                new PersonalizedCuratorTools(
                        currentUserProvider,
                        likedTrackService,
                        playbackService
                );
    }

    @Test
    void likedTracksUseAuthenticatedUserId() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        @SuppressWarnings("unchecked")
        PageResponse<LikedTrackResponse> expected =
                org.mockito.Mockito.mock(
                        PageResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                likedTrackService.getLikedTracks(
                        authenticatedUserId,
                        0,
                        10
                )
        ).thenReturn(
                expected
        );

        PageResponse<LikedTrackResponse> result =
                tools.getLikedTracks(
                        10
                );

        assertSame(
                expected,
                result
        );

        verify(
                currentUserProvider
        ).userId();

        verify(
                likedTrackService
        ).getLikedTracks(
                authenticatedUserId,
                0,
                10
        );
    }

    @Test
    void recentlyPlayedUsesAuthenticatedUserId() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        @SuppressWarnings("unchecked")
        PageResponse<PlaybackEventResponse> expected =
                org.mockito.Mockito.mock(
                        PageResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                playbackService.getRecentlyPlayed(
                        authenticatedUserId,
                        0,
                        8
                )
        ).thenReturn(
                expected
        );

        PageResponse<PlaybackEventResponse> result =
                tools.getRecentlyPlayed(
                        8
                );

        assertSame(
                expected,
                result
        );

        verify(
                playbackService
        ).getRecentlyPlayed(
                authenticatedUserId,
                0,
                8
        );
    }

    @Test
    void likedTrackSizeIsCappedAtTwenty() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        @SuppressWarnings("unchecked")
        PageResponse<LikedTrackResponse> expected =
                org.mockito.Mockito.mock(
                        PageResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                likedTrackService.getLikedTracks(
                        authenticatedUserId,
                        0,
                        20
                )
        ).thenReturn(
                expected
        );

        tools.getLikedTracks(
                500
        );

        verify(
                likedTrackService
        ).getLikedTracks(
                authenticatedUserId,
                0,
                20
        );
    }

    @Test
    void recentlyPlayedSizeIsAtLeastOne() {

        UUID authenticatedUserId =
                UUID.randomUUID();

        @SuppressWarnings("unchecked")
        PageResponse<PlaybackEventResponse> expected =
                org.mockito.Mockito.mock(
                        PageResponse.class
                );

        when(
                currentUserProvider.userId()
        ).thenReturn(
                authenticatedUserId
        );

        when(
                playbackService.getRecentlyPlayed(
                        authenticatedUserId,
                        0,
                        1
                )
        ).thenReturn(
                expected
        );

        tools.getRecentlyPlayed(
                0
        );

        verify(
                playbackService
        ).getRecentlyPlayed(
                authenticatedUserId,
                0,
                1
        );
    }
}
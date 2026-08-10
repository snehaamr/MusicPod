package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicpod.common.api.PageResponse;
import com.musicpod.library.likedtrack.LikedTrackResponse;
import com.musicpod.library.likedtrack.LikedTrackService;
import com.musicpod.playback.PlaybackEventResponse;
import com.musicpod.playback.PlaybackService;
import com.musicpod.recommendation.RecommendationResponse;
import com.musicpod.recommendation.RecommendationService;

@ExtendWith(MockitoExtension.class)
class MusicPodMcpPersonalizedToolsTests {

    private static final UUID USER_ID =
            UUID.randomUUID();

    @Mock
    private LikedTrackService
            likedTrackService;

    @Mock
    private PlaybackService
            playbackService;

    @Mock
    private McpAuditService
            mcpAuditService;
    
    @Mock
    private RecommendationService recommendationService;

    private MusicPodMcpPersonalizedTools
            tools;

    @BeforeEach
    void setUp() {

        when(
                mcpAuditService.execute(
                        anyString(),
                        anyBoolean(),
                        anyString(),
                        any(),
                        any()
                )
        ).thenAnswer(invocation -> {

            @SuppressWarnings("unchecked")
            Function<UUID, Object> operation =
                    invocation.getArgument(4);

            return operation.apply(
                    USER_ID
            );
        });

        tools =
                new MusicPodMcpPersonalizedTools(
                        likedTrackService,
                        playbackService,
                        mcpAuditService,
                        recommendationService
                );
    }

    @Test
    void likedTracksUsesAuthenticatedUser() {

        @SuppressWarnings("unchecked")
        PageResponse<LikedTrackResponse> expected =
                mock(
                        PageResponse.class
                );

        when(
                likedTrackService.getLikedTracks(
                        USER_ID,
                        0,
                        10
                )
        ).thenReturn(
                expected
        );

        PageResponse<LikedTrackResponse> result =
                tools.getLikedTracks(
                        null,
                        null
                );

        assertSame(
                expected,
                result
        );

        verify(
                likedTrackService
        ).getLikedTracks(
                USER_ID,
                0,
                10
        );
    }

    @Test
    void likedTracksUsesProvidedPagination() {

        @SuppressWarnings("unchecked")
        PageResponse<LikedTrackResponse> expected =
                mock(
                        PageResponse.class
                );

        when(
                likedTrackService.getLikedTracks(
                        USER_ID,
                        2,
                        5
                )
        ).thenReturn(
                expected
        );

        PageResponse<LikedTrackResponse> result =
                tools.getLikedTracks(
                        2,
                        5
                );

        assertSame(
                expected,
                result
        );

        verify(
                likedTrackService
        ).getLikedTracks(
                USER_ID,
                2,
                5
        );
    }

    @Test
    void recentlyPlayedUsesAuthenticatedUser() {

        @SuppressWarnings("unchecked")
        PageResponse<PlaybackEventResponse> expected =
                mock(
                        PageResponse.class
                );

        when(
                playbackService.getRecentlyPlayed(
                        USER_ID,
                        0,
                        10
                )
        ).thenReturn(
                expected
        );

        PageResponse<PlaybackEventResponse> result =
                tools.getRecentlyPlayed(
                        null,
                        null
                );

        assertSame(
                expected,
                result
        );

        verify(
                playbackService
        ).getRecentlyPlayed(
                USER_ID,
                0,
                10
        );
    }

    @Test
    void recentlyPlayedUsesProvidedPagination() {

        @SuppressWarnings("unchecked")
        PageResponse<PlaybackEventResponse> expected =
                mock(
                        PageResponse.class
                );

        when(
                playbackService.getRecentlyPlayed(
                        USER_ID,
                        1,
                        15
                )
        ).thenReturn(
                expected
        );

        PageResponse<PlaybackEventResponse> result =
                tools.getRecentlyPlayed(
                        1,
                        15
                );

        assertSame(
                expected,
                result
        );

        verify(
                playbackService
        ).getRecentlyPlayed(
                USER_ID,
                1,
                15
        );
    }

    @Test
    void negativePageDoesNotCallLikedTrackService() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getLikedTracks(
                                -1,
                                10
                        )
        );

        verifyNoInteractions(
                likedTrackService
        );
    }

    @Test
    void invalidLikedTrackSizeDoesNotCallService() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getLikedTracks(
                                0,
                                21
                        )
        );

        verifyNoInteractions(
                likedTrackService
        );
    }

    @Test
    void negativePageDoesNotCallPlaybackService() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getRecentlyPlayed(
                                -1,
                                10
                        )
        );

        verifyNoInteractions(
                playbackService
        );
    }

    @Test
    void invalidRecentlyPlayedSizeDoesNotCallService() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getRecentlyPlayed(
                                0,
                                21
                        )
        );

        verifyNoInteractions(
                playbackService
        );
    }
    
    @Test
    void getsRecommendationsForAuthenticatedUserThroughAudit() {

        List<RecommendationResponse> expected =
                List.of();

        when(
                recommendationService
                        .getRecommendations(
                                USER_ID,
                                10
                        )
        ).thenReturn(
                expected
        );

        List<RecommendationResponse> actual =
                tools.getRecommendations(
                        10
                );

        assertSame(
                expected,
                actual
        );

        verify(
                recommendationService
        ).getRecommendations(
                USER_ID,
                10
        );

        verify(
                mcpAuditService
        ).execute(
                eq("musicpod_get_recommendations"),
                eq(false),
                eq(McpAuditService.RISK_READ_ONLY),
                eq(
                        Map.of(
                                "size",
                                10
                        )
                ),
                any()
        );
    }
}
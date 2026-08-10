package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;
import com.musicpod.library.playlist.PlaylistResponse;
import com.musicpod.library.playlist.PlaylistService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class MusicPodMcpResourcesTests {

    private static final UUID USER_ID =
            UUID.randomUUID();

    @Mock
    private TrackService
            trackService;

    @Mock
    private PlaylistService
            playlistService;

    @Mock
    private JsonMapper
            jsonMapper;

    @Mock
    private McpAuditService
            mcpAuditService;

    private MusicPodMcpResources
            resources;

    @BeforeEach
    void setUp() {

        when(
                mcpAuditService.executeJson(
                        anyString(),
                        anyBoolean(),
                        anyString(),
                        any(),
                        any()
                )
        ).thenAnswer(invocation -> {

            @SuppressWarnings("unchecked")
            Function<UUID, String> operation =
                    invocation.getArgument(4);

            return operation.apply(
                    USER_ID
            );
        });

        resources =
                new MusicPodMcpResources(
                        trackService,
                        playlistService,
                        jsonMapper,
                        mcpAuditService
                );
    }

    @Test
    void trackResourceReturnsSerializedTrack() {

        UUID trackId =
                UUID.randomUUID();

        TrackResponse track =
                org.mockito.Mockito.mock(
                        TrackResponse.class
                );

        String expectedJson =
                """
                {"id":"track"}
                """.trim();

        when(
                trackService.getById(
                        trackId
                )
        ).thenReturn(
                track
        );

        when(
                jsonMapper.writeValueAsString(
                        track
                )
        ).thenReturn(
                expectedJson
        );

        String result =
                resources.getTrack(
                        trackId.toString()
                );

        assertEquals(
                expectedJson,
                result
        );

        verify(
                trackService
        ).getById(
                trackId
        );
    }

    @Test
    void ownedPlaylistUsesAuthenticatedUser() {

        UUID playlistId =
                UUID.randomUUID();

        PlaylistResponse playlist =
                new PlaylistResponse(
                        playlistId,
                        "Road Trip",
                        "Driving music",
                        Instant.now(),
                        Instant.now()
                );

        String expectedJson =
                """
                {"id":"playlist"}
                """.trim();

        when(
                playlistService.getById(
                        USER_ID,
                        playlistId
                )
        ).thenReturn(
                playlist
        );

        when(
                jsonMapper.writeValueAsString(
                        playlist
                )
        ).thenReturn(
                expectedJson
        );

        String result =
                resources.getOwnedPlaylist(
                        playlistId.toString()
                );

        assertEquals(
                expectedJson,
                result
        );

        verify(
                playlistService
        ).getById(
                USER_ID,
                playlistId
        );
    }

    @Test
    void invalidTrackIdDoesNotReadTrack() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        resources.getTrack(
                                "not-a-uuid"
                        )
        );

        verifyNoInteractions(
                trackService
        );
    }

    @Test
    void blankTrackIdDoesNotReadTrack() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        resources.getTrack(
                                "   "
                        )
        );

        verifyNoInteractions(
                trackService
        );
    }

    @Test
    void invalidPlaylistIdDoesNotReadPlaylist() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        resources.getOwnedPlaylist(
                                "not-a-uuid"
                        )
        );

        verifyNoInteractions(
                playlistService
        );
    }

    @Test
    void blankPlaylistIdDoesNotReadPlaylist() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        resources.getOwnedPlaylist(
                                "   "
                        )
        );

        verifyNoInteractions(
                playlistService
        );
    }
}
package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.library.playlist.CreatePlaylistRequest;
import com.musicpod.library.playlist.PlaylistResponse;
import com.musicpod.library.playlist.PlaylistService;
import com.musicpod.library.playlist.PlaylistTrackResponse;

@ExtendWith(MockitoExtension.class)
class MusicPodMcpWriteToolsTests {

    private static final UUID USER_ID =
            UUID.randomUUID();

    @Mock
    private PlaylistService
            playlistService;

    @Mock
    private McpAuditService
            mcpAuditService;

    private MusicPodMcpWriteTools
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
                new MusicPodMcpWriteTools(
                        playlistService,
                        mcpAuditService
                );
    }

    @Test
    void createPlaylistUsesAuthenticatedUser() {

        UUID playlistId =
                UUID.randomUUID();

        PlaylistResponse expected =
                new PlaylistResponse(
                        playlistId,
                        "Road Trip",
                        "Music for driving",
                        Instant.now(),
                        Instant.now()
                );

        when(
                playlistService.create(
                        USER_ID,
                        new CreatePlaylistRequest(
                                "Road Trip",
                                "Music for driving"
                        )
                )
        ).thenReturn(
                expected
        );

        PlaylistResponse result =
                tools.createPlaylist(
                        "Road Trip",
                        "Music for driving"
                );

        assertSame(
                expected,
                result
        );

        verify(
                playlistService
        ).create(
                USER_ID,
                new CreatePlaylistRequest(
                        "Road Trip",
                        "Music for driving"
                )
        );

        verify(
                mcpAuditService
        ).execute(
                anyString(),
                anyBoolean(),
                anyString(),
                any(),
                any()
        );
    }

    @Test
    void addTracksUsesAuthenticatedUser() {

        UUID playlistId =
                UUID.randomUUID();

        UUID track1 =
                UUID.randomUUID();

        UUID track2 =
                UUID.randomUUID();

        List<PlaylistTrackResponse> added =
                List.of();

        when(
                playlistService.addTracks(
                        USER_ID,
                        playlistId,
                        List.of(
                                track1,
                                track2
                        )
                )
        ).thenReturn(
                added
        );

        AddTracksToPlaylistResponse result =
                tools.addTracksToPlaylist(
                        playlistId.toString(),
                        List.of(
                                track1.toString(),
                                track2.toString()
                        )
                );

        assertEquals(
                playlistId,
                result.playlistId()
        );

        assertSame(
                added,
                result.tracks()
        );

        verify(
                playlistService
        ).addTracks(
                USER_ID,
                playlistId,
                List.of(
                        track1,
                        track2
                )
        );
    }

    @Test
    void duplicateTrackIdsAreRemoved() {

        UUID playlistId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        when(
                playlistService.addTracks(
                        USER_ID,
                        playlistId,
                        List.of(trackId)
                )
        ).thenReturn(
                List.of()
        );

        tools.addTracksToPlaylist(
                playlistId.toString(),
                List.of(
                        trackId.toString(),
                        trackId.toString()
                )
        );

        verify(
                playlistService
        ).addTracks(
                USER_ID,
                playlistId,
                List.of(trackId)
        );
    }

    @Test
    void invalidPlaylistIdDoesNotWrite() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.addTracksToPlaylist(
                                "not-a-uuid",
                                List.of(
                                        UUID.randomUUID()
                                                .toString()
                                )
                        )
        );

        verifyNoInteractions(
                playlistService
        );
    }

    @Test
    void emptyTrackListDoesNotWrite() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.addTracksToPlaylist(
                                UUID.randomUUID()
                                        .toString(),
                                List.of()
                        )
        );

        verifyNoInteractions(
                playlistService
        );
    }

    @Test
    void blankPlaylistNameDoesNotWrite() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.createPlaylist(
                                "   ",
                                null
                        )
        );

        verifyNoInteractions(
                playlistService
        );
    }
}
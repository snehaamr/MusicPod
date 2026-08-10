package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;
import com.musicpod.search.track.HybridTrackSearchService;
import com.musicpod.search.track.TrackSearchResult;

@ExtendWith(MockitoExtension.class)
class MusicPodMcpReadToolsTests {

    @Mock
    private HybridTrackSearchService
            hybridTrackSearchService;

    @Mock
    private TrackService
            trackService;

    @Mock
    private McpAuditService
            mcpAuditService;

    private MusicPodMcpReadTools
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
                    UUID.randomUUID()
            );
        });

        tools =
                new MusicPodMcpReadTools(
                        hybridTrackSearchService,
                        trackService,
                        mcpAuditService
                );
    }

    @Test
    void searchTracksUsesProvidedSize() {

        List<TrackSearchResult> expected =
                List.of();

        when(
                hybridTrackSearchService.search(
                        "Coldplay",
                        5
                )
        ).thenReturn(
                expected
        );

        List<TrackSearchResult> result =
                tools.searchTracks(
                        "Coldplay",
                        5
                );

        assertSame(
                expected,
                result
        );

        verify(
                hybridTrackSearchService
        ).search(
                "Coldplay",
                5
        );
    }

    @Test
    void searchTracksUsesDefaultSize() {

        List<TrackSearchResult> expected =
                List.of();

        when(
                hybridTrackSearchService.search(
                        "Coldplay",
                        10
                )
        ).thenReturn(
                expected
        );

        List<TrackSearchResult> result =
                tools.searchTracks(
                        "Coldplay",
                        null
                );

        assertSame(
                expected,
                result
        );

        verify(
                hybridTrackSearchService
        ).search(
                "Coldplay",
                10
        );
    }

    @Test
    void getTrackUsesParsedUuid() {

        UUID trackId =
                UUID.randomUUID();

        TrackResponse expected =
                org.mockito.Mockito.mock(
                        TrackResponse.class
                );

        when(
                trackService.getById(
                        trackId
                )
        ).thenReturn(
                expected
        );

        TrackResponse result =
                tools.getTrack(
                        trackId.toString()
                );

        assertSame(
                expected,
                result
        );

        verify(
                trackService
        ).getById(
                trackId
        );
    }

    @Test
    void invalidTrackIdDoesNotCallTrackService() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getTrack(
                                "not-a-uuid"
                        )
        );

        verifyNoInteractions(
                trackService
        );
    }

    @Test
    void blankTrackIdDoesNotCallTrackService() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        tools.getTrack(
                                "   "
                        )
        );

        verifyNoInteractions(
                trackService
        );
    }
}
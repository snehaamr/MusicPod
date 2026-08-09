package com.musicpod.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

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
    private TrackService trackService;

    private MusicPodMcpReadTools tools;

    @BeforeEach
    void setUp() {

        tools =
                new MusicPodMcpReadTools(
                        hybridTrackSearchService,
                        trackService
                );
    }

    @Test
    void searchUsesDefaultSizeWhenSizeIsMissing() {

        when(
                hybridTrackSearchService.search(
                        "songs by Queen",
                        10
                )
        ).thenReturn(
                List.of()
        );

        List<TrackSearchResult> result =
                tools.searchTracks(
                        "songs by Queen",
                        null
                );

        assertEquals(
                List.of(),
                result
        );

        verify(
                hybridTrackSearchService
        ).search(
                "songs by Queen",
                10
        );
    }

    @Test
    void searchUsesRequestedSize() {

        when(
                hybridTrackSearchService.search(
                        "romantic Kishore Kumar songs",
                        5
                )
        ).thenReturn(
                List.of()
        );

        tools.searchTracks(
                "romantic Kishore Kumar songs",
                5
        );

        verify(
                hybridTrackSearchService
        ).search(
                "romantic Kishore Kumar songs",
                5
        );
    }

    @Test
    void getTrackUsesParsedUuid() {

        UUID trackId =
                UUID.randomUUID();

        TrackResponse response =
                org.mockito.Mockito.mock(
                        TrackResponse.class
                );

        when(
                trackService.getById(
                        trackId
                )
        ).thenReturn(
                response
        );

        TrackResponse result =
                tools.getTrack(
                        trackId.toString()
                );

        assertEquals(
                response,
                result
        );

        verify(
                trackService
        ).getById(
                trackId
        );
    }

    @Test
    void getTrackRejectsInvalidUuid() {

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
    void getTrackRejectsBlankTrackId() {

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
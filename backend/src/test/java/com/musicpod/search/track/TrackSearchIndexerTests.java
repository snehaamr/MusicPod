package com.musicpod.search.track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.opensearch.client.opensearch.OpenSearchClient;

import com.musicpod.catalog.track.Track;

@ExtendWith(MockitoExtension.class)
class TrackSearchIndexerTests {

    @Mock
    private OpenSearchClient openSearchClient;

    @Mock
    private TrackSearchSourceRepository sourceRepository;

    @Mock
    private TrackSemanticTextBuilder semanticTextBuilder;

    @Mock
    private TrackEmbeddingService embeddingService;

    private TrackSearchIndexer indexer;

    @BeforeEach
    void setUp() {

        indexer =
                new TrackSearchIndexer(
                        openSearchClient,
                        sourceRepository,
                        semanticTextBuilder,
                        embeddingService
                );
    }

    @Test
    void emptyCatalogBackfillIndexesNothing() {

        when(
                sourceRepository.findAllForSearch()
        ).thenReturn(
                List.of()
        );

        SearchBackfillResponse response =
                indexer.backfill();

        assertEquals(
                0,
                response.indexedTracks()
        );

        verifyNoInteractions(
                semanticTextBuilder,
                embeddingService,
                openSearchClient
        );
    }

    @Test
    void failsWhenEmbeddingCountDoesNotMatchTracks() {

        Track track =
                org.mockito.Mockito.mock(
                        Track.class
                );

        when(
                sourceRepository.findAllForSearch()
        ).thenReturn(
                List.of(
                        track
                )
        );

        when(
                semanticTextBuilder.build(
                        track
                )
        ).thenReturn(
                "semantic text"
        );

        when(
                embeddingService.embedAll(
                        List.of(
                                "semantic text"
                        )
                )
        ).thenReturn(
                List.of()
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        indexer.backfill()
        );

        verifyNoInteractions(
                openSearchClient
        );
    }
}
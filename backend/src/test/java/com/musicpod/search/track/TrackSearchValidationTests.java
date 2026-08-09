package com.musicpod.search.track;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;

@ExtendWith(MockitoExtension.class)
class TrackSearchValidationTests {

    @Mock
    private OpenSearchClient openSearchClient;

    @Mock
    private TrackEmbeddingService trackEmbeddingService;

    private TrackSearchService lexicalSearchService;

    private SemanticTrackSearchService semanticSearchService;

    @BeforeEach
    void setUp() {

        lexicalSearchService =
                new TrackSearchService(
                        openSearchClient
                );

        semanticSearchService =
                new SemanticTrackSearchService(
                        openSearchClient,
                        trackEmbeddingService
                );
    }

    @Test
    void lexicalSearchRejectsBlankQuery() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        lexicalSearchService.search(
                                "   ",
                                20
                        )
        );

        verifyNoInteractions(
                openSearchClient
        );
    }

    @Test
    void lexicalSearchRejectsSizeBelowOne() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        lexicalSearchService.search(
                                "queen",
                                0
                        )
        );

        verifyNoInteractions(
                openSearchClient
        );
    }

    @Test
    void lexicalSearchRejectsSizeAboveFifty() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        lexicalSearchService.search(
                                "queen",
                                51
                        )
        );

        verifyNoInteractions(
                openSearchClient
        );
    }

    @Test
    void semanticSearchRejectsBlankQueryBeforeEmbedding() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        semanticSearchService.search(
                                "",
                                20
                        )
        );

        verifyNoInteractions(
                trackEmbeddingService,
                openSearchClient
        );
    }

    @Test
    void semanticSearchRejectsInvalidSizeBeforeEmbedding() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        semanticSearchService.search(
                                "romantic songs",
                                51
                        )
        );

        verifyNoInteractions(
                trackEmbeddingService,
                openSearchClient
        );
    }
}
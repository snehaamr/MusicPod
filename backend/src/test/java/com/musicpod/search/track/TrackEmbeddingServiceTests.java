package com.musicpod.search.track;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class TrackEmbeddingServiceTests {

    @Mock
    private ObjectProvider<EmbeddingModel>
            embeddingModelProvider;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingResponse embeddingResponse;

    @Mock
    private Embedding embedding;

    private TrackEmbeddingService service;

    @BeforeEach
    void setUp() {

        service =
                new TrackEmbeddingService(
                        embeddingModelProvider,
                        3,
                        "test-embedding-model"
                );
    }

    @Test
    void returnsEmbeddingVector() {

        when(
                embeddingModelProvider.getIfAvailable()
        ).thenReturn(
                embeddingModel
        );

        when(
                embeddingModel.call(
                        any(
                                EmbeddingRequest.class
                        )
                )
        ).thenReturn(
                embeddingResponse
        );

        when(
                embeddingResponse.getResults()
        ).thenReturn(
                List.of(
                        embedding
                )
        );

        when(
                embeddingResponse.getResult()
        ).thenReturn(
                embedding
        );

        when(
                embedding.getOutput()
        ).thenReturn(
                new float[] {
                        0.1f,
                        0.2f,
                        0.3f
                }
        );

        List<Float> result =
                service.embed(
                        "Queen"
                );

        assertEquals(
                List.of(
                        0.1f,
                        0.2f,
                        0.3f
                ),
                result
        );
    }

    @Test
    void rejectsEmbeddingDimensionMismatch() {

        when(
                embeddingModelProvider.getIfAvailable()
        ).thenReturn(
                embeddingModel
        );

        when(
                embeddingModel.call(
                        any(
                                EmbeddingRequest.class
                        )
                )
        ).thenReturn(
                embeddingResponse
        );

        when(
                embeddingResponse.getResults()
        ).thenReturn(
                List.of(
                        embedding
                )
        );

        when(
                embeddingResponse.getResult()
        ).thenReturn(
                embedding
        );

        when(
                embedding.getOutput()
        ).thenReturn(
                new float[] {
                        0.1f,
                        0.2f
                }
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.embed(
                                "Queen"
                        )
        );
    }

    @Test
    void embedAllRestoresOriginalInputOrder() {

        Embedding indexZero =
                mock(
                        Embedding.class
                );

        Embedding indexOne =
                mock(
                        Embedding.class
                );

        when(
                indexZero.getIndex()
        ).thenReturn(
                0
        );

        when(
                indexOne.getIndex()
        ).thenReturn(
                1
        );

        when(
                indexZero.getOutput()
        ).thenReturn(
                new float[] {
                        1f,
                        2f,
                        3f
                }
        );

        when(
                indexOne.getOutput()
        ).thenReturn(
                new float[] {
                        4f,
                        5f,
                        6f
                }
        );

        when(
                embeddingModelProvider.getIfAvailable()
        ).thenReturn(
                embeddingModel
        );

        when(
                embeddingModel.call(
                        any(
                                EmbeddingRequest.class
                        )
                )
        ).thenReturn(
                embeddingResponse
        );

        /*
         * Deliberately return them backwards.
         */
        when(
                embeddingResponse.getResults()
        ).thenReturn(
                List.of(
                        indexOne,
                        indexZero
                )
        );

        List<List<Float>> result =
                service.embedAll(
                        List.of(
                                "first",
                                "second"
                        )
                );

        assertEquals(
                List.of(
                        1f,
                        2f,
                        3f
                ),
                result.get(0)
        );

        assertEquals(
                List.of(
                        4f,
                        5f,
                        6f
                ),
                result.get(1)
        );
    }

    @Test
    void failsWhenNoEmbeddingModelIsConfigured() {

        when(
                embeddingModelProvider.getIfAvailable()
        ).thenReturn(
                null
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        service.embed(
                                "Queen"
                        )
        );
    }
}
package com.musicpod.search.track;

import java.io.IOException;
import java.util.List;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;

import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
import com.musicpod.search.SearchIndexNames;

@Service
public class TrackSearchProjectionService {

    private final OpenSearchClient
            openSearchClient;

    private final TrackSemanticTextBuilder
            semanticTextBuilder;

    private final TrackEmbeddingService
            trackEmbeddingService;

    public TrackSearchProjectionService(
            OpenSearchClient openSearchClient,
            TrackSemanticTextBuilder
                    semanticTextBuilder,
            TrackEmbeddingService
                    trackEmbeddingService) {

        this.openSearchClient =
                openSearchClient;

        this.semanticTextBuilder =
                semanticTextBuilder;

        this.trackEmbeddingService =
                trackEmbeddingService;
    }

    public void upsert(
            TrackSearchUpsertedEvent event) {

        String semanticText =
                semanticTextBuilder
                        .build(event);

        List<Float> embedding =
                trackEmbeddingService
                        .embed(
                                semanticText
                        );

        TrackSearchDocument document =
                TrackSearchDocument.from(
                        event,
                        semanticText,
                        embedding
                );

        try {

            openSearchClient.index(
                    request ->
                            request
                                    .index(
                                            SearchIndexNames.TRACKS
                                    )
                                    .id(
                                            event.trackId()
                                                    .toString()
                                    )
                                    .document(
                                            document
                                    )
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to update track search projection",
                    exception
            );
        }
    }

    public void delete(
            TrackSearchDeletedEvent event) {

        try {

            openSearchClient.delete(
                    request ->
                            request
                                    .index(
                                            SearchIndexNames.TRACKS
                                    )
                                    .id(
                                            event.trackId()
                                                    .toString()
                                    )
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to delete track search projection",
                    exception
            );
        }
    }
}
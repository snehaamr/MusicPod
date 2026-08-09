package com.musicpod.search.track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;

import org.springframework.stereotype.Service;

import com.musicpod.catalog.track.Track;
import com.musicpod.search.SearchIndexNames;

@Service
public class TrackSearchIndexer {

    private final OpenSearchClient
            openSearchClient;

    private final TrackSearchSourceRepository
            trackSearchSourceRepository;

    private final TrackSemanticTextBuilder
            semanticTextBuilder;

    private final TrackEmbeddingService
            trackEmbeddingService;

    public TrackSearchIndexer(
            OpenSearchClient openSearchClient,
            TrackSearchSourceRepository
                    trackSearchSourceRepository,
            TrackSemanticTextBuilder
                    semanticTextBuilder,
            TrackEmbeddingService
                    trackEmbeddingService) {

        this.openSearchClient =
                openSearchClient;

        this.trackSearchSourceRepository =
                trackSearchSourceRepository;

        this.semanticTextBuilder =
                semanticTextBuilder;

        this.trackEmbeddingService =
                trackEmbeddingService;
    }

    public SearchBackfillResponse backfill() {

        List<Track> tracks =
                trackSearchSourceRepository
                        .findAllForSearch();

        if (tracks.isEmpty()) {

            return new SearchBackfillResponse(
                    0
            );
        }

        List<String> semanticTexts =
                new ArrayList<>(
                        tracks.size()
                );

        for (Track track : tracks) {

            semanticTexts.add(
                    semanticTextBuilder
                            .build(track)
            );
        }

        List<List<Float>> embeddings =
                trackEmbeddingService
                        .embedAll(
                                semanticTexts
                        );

        if (embeddings.size()
                != tracks.size()) {

            throw new IllegalStateException(
                    "Embedding count does not match track count"
            );
        }

        List<BulkOperation> operations =
                new ArrayList<>(
                        tracks.size()
                );

        for (int index = 0;
             index < tracks.size();
             index++) {

            Track track =
                    tracks.get(index);

            TrackSearchDocument document =
                    TrackSearchDocument.from(
                            track,
                            semanticTexts.get(index),
                            embeddings.get(index)
                    );

            IndexOperation<TrackSearchDocument>
                    indexOperation =
                    new IndexOperation
                            .Builder<TrackSearchDocument>()
                            .index(
                                    SearchIndexNames
                                            .TRACKS_REINDEX_TARGET
                            )
                            .id(
                                    document
                                            .trackId()
                                            .toString()
                            )
                            .document(document)
                            .build();

            operations.add(
                    new BulkOperation
                            .Builder()
                            .index(
                                    indexOperation
                            )
                            .build()
            );
        }

        try {

            BulkResponse response =
                    openSearchClient.bulk(
                            builder ->
                                    builder
                                            .operations(
                                                    operations
                                            )
                                            .refresh(
                                                    Refresh.WaitFor
                                            )
                    );

            if (response.errors()) {

                StringBuilder failures =
                        new StringBuilder();

                response.items()
                        .forEach(item -> {

                            if (item.error() != null) {

                                failures
                                        .append("id=")
                                        .append(item.id())
                                        .append(", status=")
                                        .append(item.status())
                                        .append(", type=")
                                        .append(item.error().type())
                                        .append(", reason=")
                                        .append(item.error().reason())
                                        .append(System.lineSeparator());
                            }
                        });

                throw new IllegalStateException(
                        "One or more tracks failed "
                                + "to index in OpenSearch:"
                                + System.lineSeparator()
                                + failures
                );
            }

            return new SearchBackfillResponse(
                    tracks.size()
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to backfill OpenSearch",
                    exception
            );
        }
    }
}
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
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.catalog.track.Track;
import com.musicpod.search.SearchIndexNames;

@Service
public class TrackSearchIndexer {

    private final OpenSearchClient openSearchClient;

    private final TrackSearchSourceRepository
            trackSearchSourceRepository;

    public TrackSearchIndexer(
            OpenSearchClient openSearchClient,
            TrackSearchSourceRepository
                    trackSearchSourceRepository) {

        this.openSearchClient =
                openSearchClient;

        this.trackSearchSourceRepository =
                trackSearchSourceRepository;
    }

    @Transactional(readOnly = true)
    public SearchBackfillResponse backfill() {

        List<Track> tracks =
                trackSearchSourceRepository
                        .findAllForSearch();

        if (tracks.isEmpty()) {

            return new SearchBackfillResponse(
                    0
            );
        }

        List<BulkOperation> operations =
                new ArrayList<>(
                        tracks.size()
                );

        for (Track track : tracks) {

            TrackSearchDocument document =
                    TrackSearchDocument.from(
                            track
                    );

            IndexOperation<TrackSearchDocument>
                    indexOperation =
                    new IndexOperation
                            .Builder<TrackSearchDocument>()
                            .index(
                                    SearchIndexNames.TRACKS
                            )
                            .id(
                                    document
                                            .trackId()
                                            .toString()
                            )
                            .document(
                                    document
                            )
                            .build();

            operations.add(
                    new BulkOperation.Builder()
                            .index(indexOperation)
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

                throw new IllegalStateException(
                        "One or more tracks failed "
                                + "to index in OpenSearch"
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
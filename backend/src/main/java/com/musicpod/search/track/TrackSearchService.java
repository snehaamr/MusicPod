package com.musicpod.search.track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import org.springframework.stereotype.Service;

import com.musicpod.search.SearchIndexNames;

@Service
public class TrackSearchService {

    private static final int MAX_RESULTS =
            50;

    private final OpenSearchClient
            openSearchClient;

    public TrackSearchService(
            OpenSearchClient openSearchClient) {

        this.openSearchClient =
                openSearchClient;
    }

    public List<TrackSearchResult> search(
            String query,
            int size) {

        String normalizedQuery =
                normalizeQuery(query);

        int normalizedSize =
                normalizeSize(size);

        try {

            SearchResponse<TrackSearchDocument>
                    response =
                    openSearchClient.search(
                            search ->
                                    search
                                            .index(
                                                    SearchIndexNames.TRACKS
                                            )
                                            .size(
                                                    normalizedSize
                                            )
                                            .query(
                                                    q ->
                                                            q.multiMatch(
                                                                    multiMatch ->
                                                                            multiMatch
                                                                                    .query(
                                                                                            normalizedQuery
                                                                                    )
                                                                                    .fields(
                                                                                            List.of(
                                                                                                    "title^4",
                                                                                                    "artistName^3",
                                                                                                    "albumTitle^2"
                                                                                            )
                                                                                    )
                                                                                    .fuzziness(
                                                                                            "AUTO"
                                                                                    )
                                                            )
                                            ),
                            TrackSearchDocument.class
                    );

            List<TrackSearchResult> results =
                    new ArrayList<>();

            for (Hit<TrackSearchDocument> hit :
                    response.hits().hits()) {

                TrackSearchDocument document =
                        hit.source();

                if (document == null) {
                    continue;
                }

                double score =
                        hit.score() == null
                                ? 0.0
                                : hit.score();

                results.add(
                        TrackSearchResult.from(
                                document,
                                score
                        )
                );
            }

            return results;

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "OpenSearch query failed",
                    exception
            );
        }
    }

    private String normalizeQuery(
            String query) {

        if (query == null
                || query.isBlank()) {

            throw new IllegalArgumentException(
                    "Search query must not be blank"
            );
        }

        return query.trim();
    }

    private int normalizeSize(
            int size) {

        if (size < 1
                || size > MAX_RESULTS) {

            throw new IllegalArgumentException(
                    "Search size must be between 1 and "
                            + MAX_RESULTS
            );
        }

        return size;
    }
}
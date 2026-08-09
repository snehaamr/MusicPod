package com.musicpod.search.track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import org.springframework.stereotype.Service;

import com.musicpod.search.SearchIndexNames;

@Service
public class HybridTrackSearchService {

    private static final int MAX_RESULTS =
            50;

    private static final String HYBRID_PIPELINE =
            "musicpod-hybrid-search-v1";

    private final OpenSearchClient
            openSearchClient;

    private final TrackEmbeddingService
            trackEmbeddingService;

    public HybridTrackSearchService(
            OpenSearchClient openSearchClient,
            TrackEmbeddingService
                    trackEmbeddingService) {

        this.openSearchClient =
                openSearchClient;

        this.trackEmbeddingService =
                trackEmbeddingService;
    }

    public List<TrackSearchResult> search(
            String query,
            int size) {

        String normalizedQuery =
                normalizeQuery(query);

        int normalizedSize =
                normalizeSize(size);

        /*
         * Generate the semantic representation of
         * the user's query.
         */
        List<Float> queryEmbedding =
                trackEmbeddingService.embed(
                        normalizedQuery
                );

        /*
         * Query #1:
         *
         * Traditional lexical/BM25 search.
         *
         * Track title receives the strongest boost,
         * followed by artist and then album.
         */
        Query lexicalQuery =
                Query.of(
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
                                                                        "albumTitle^2",
                                                                        "semanticText"
                                                                )
                                                        )
                                                        .fuzziness(
                                                                "AUTO"
                                                        )
                                )
                );

        /*
         * Query #2:
         *
         * Vector similarity search.
         *
         * Search for tracks whose stored embedding
         * is closest to the query embedding.
         */
        Query semanticQuery =
                Query.of(
                        q ->
                                q.knn(
                                        knn ->
                                                knn
                                                        .field(
                                                                "embedding"
                                                        )
                                                        .vector(
                                                                queryEmbedding
                                                        )
                                                        .k(
                                                                candidateCount(
                                                                        normalizedSize
                                                                )
                                                        )
                                )
                );

        try {

            SearchResponse<TrackSearchDocument>
                    response =
                    openSearchClient.search(
                            search ->
                                    search
                                            .index(
                                                    SearchIndexNames.TRACKS
                                            )
                                            .pipeline(
                                                    HYBRID_PIPELINE
                                            )
                                            .size(
                                                    normalizedSize
                                            )
                                            .query(
                                                    q ->
                                                            q.hybrid(
                                                                    hybrid ->
                                                                            hybrid
                                                                                    .queries(
                                                                                            List.of(
                                                                                                    lexicalQuery,
                                                                                                    semanticQuery
                                                                                            )
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
                    "Hybrid OpenSearch query failed",
                    exception
            );
        }
    }

    private int candidateCount(
            int requestedSize) {

        /*
         * Give semantic search more candidates
         * than the final result size so RRF has
         * a larger pool to combine.
         */
        return Math.min(
                100,
                Math.max(
                        20,
                        requestedSize * 5
                )
        );
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
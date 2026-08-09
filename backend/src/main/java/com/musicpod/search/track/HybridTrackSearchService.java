package com.musicpod.search.track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import com.musicpod.search.SearchIndexNames;

@Service
public class HybridTrackSearchService {

    private static final int MAX_RESULTS = 50;

    private static final String HYBRID_PIPELINE =
            "musicpod-hybrid-search-v1";

    private static final int ARTIST_RESOLUTION_SIZE = 20;

    private final OpenSearchClient openSearchClient;

    private final TrackEmbeddingService trackEmbeddingService;

    public HybridTrackSearchService(
            OpenSearchClient openSearchClient,
            TrackEmbeddingService trackEmbeddingService) {

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
         * First determine whether the user's query
         * explicitly mentions one of the artists
         * already known to MusicPod.
         *
         * Examples:
         *
         * "Kishore Kumar"
         * "romantic Kishore Kumar songs"
         * "songs by Queen"
         *
         * If an artist is explicitly mentioned,
         * all hybrid retrieval is restricted to
         * that artist.
         */
        Optional<String> resolvedArtist =
                resolveArtistMention(
                        normalizedQuery
                );

        List<Float> queryEmbedding =
                trackEmbeddingService.embed(
                        normalizedQuery
                );

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

            SearchResponse<TrackSearchDocument> response;

            if (resolvedArtist.isPresent()) {

                Query artistFilter =
                        buildArtistFilter(
                                resolvedArtist.get()
                        );

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
                                                                                        .filter(
                                                                                                artistFilter
                                                                                        )
                                                                )
                                                ),
                                TrackSearchDocument.class
                        );

            } else {

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
            }

            return mapResults(
                    response
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Hybrid OpenSearch query failed",
                    exception
            );
        }
    }

    /*
     * Search our indexed catalog to determine
     * whether the user explicitly mentioned
     * a known artist.
     *
     * We deliberately return the canonical
     * artist name stored in OpenSearch.
     *
     * That means:
     *
     * query:
     *   "romantic kishore kumar songs"
     *
     * resolves to:
     *   "Kishore Kumar"
     *
     * which can then be safely used against
     * artistName.keyword.
     */
    private Optional<String> resolveArtistMention(
            String query) {

        try {

            SearchResponse<TrackSearchDocument> response =
                    openSearchClient.search(
                            search ->
                                    search
                                            .index(
                                                    SearchIndexNames.TRACKS
                                            )
                                            .size(
                                                    ARTIST_RESOLUTION_SIZE
                                            )
                                            .query(
                                                    q ->
                                                            q.match(
                                                                    match ->
                                                                            match
                                                                                    .field(
                                                                                            "artistName"
                                                                                    )
                                                                                    .query(
                                                                                            FieldValue.of(
                                                                                                    query
                                                                                            )
                                                                                    )
                                                            )
                                            ),
                            TrackSearchDocument.class
                    );

            String normalizedUserQuery =
                    query.toLowerCase(
                            Locale.ROOT
                    );

            /*
             * Find the longest artist name that
             * appears in the query.
             *
             * Longest wins in case names overlap.
             */
            String bestMatch = null;

            for (Hit<TrackSearchDocument> hit :
                    response.hits().hits()) {

                TrackSearchDocument document =
                        hit.source();

                if (document == null
                        || document.artistName() == null) {

                    continue;
                }

                String artistName =
                        document.artistName().trim();

                String normalizedArtist =
                        artistName.toLowerCase(
                                Locale.ROOT
                        );

                if (!normalizedUserQuery.contains(
                        normalizedArtist)) {

                    continue;
                }

                if (bestMatch == null
                        || artistName.length()
                        > bestMatch.length()) {

                    bestMatch =
                            artistName;
                }
            }

            return Optional.ofNullable(
                    bestMatch
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Unable to resolve artist from search query",
                    exception
            );
        }
    }

    private Query buildArtistFilter(
            String artistName) {

        /*
         * artistName.keyword is an exact-value
         * field.
         *
         * We use the canonical value retrieved
         * from OpenSearch, so capitalization
         * matches what was indexed.
         */
        return Query.of(
                q ->
                        q.term(
                                term ->
                                        term
                                                .field(
                                                        "artistName.keyword"
                                                )
                                                .value(
                                                        FieldValue.of(
                                                                artistName
                                                        )
                                                )
                        )
        );
    }

    private List<TrackSearchResult> mapResults(
            SearchResponse<TrackSearchDocument> response) {

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
    }

    private int candidateCount(
            int requestedSize) {

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
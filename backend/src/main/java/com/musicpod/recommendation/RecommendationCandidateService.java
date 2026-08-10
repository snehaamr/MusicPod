package com.musicpod.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.musicpod.search.track.SemanticTrackSearchService;
import com.musicpod.search.track.TrackSearchResult;

@Service
public class RecommendationCandidateService {

    private static final int SEED_LIMIT =
            5;

    private static final int CANDIDATES_PER_SEED =
            12;

    private static final int MAX_RESULTS =
            50;

    private final RecommendationSignalRepository
            recommendationSignalRepository;

    private final SemanticTrackSearchService
            semanticTrackSearchService;

    public RecommendationCandidateService(
            RecommendationSignalRepository
                    recommendationSignalRepository,
            SemanticTrackSearchService
                    semanticTrackSearchService) {

        this.recommendationSignalRepository =
                recommendationSignalRepository;

        this.semanticTrackSearchService =
                semanticTrackSearchService;
    }

    public List<RecommendationCandidate>
            generateCandidates(
                    UUID userId,
                    int limit) {

        validateLimit(limit);

        /*
         * Find the user's strongest taste signals.
         */
        List<RecommendationSeed> seeds =
                recommendationSignalRepository
                        .findTopSeeds(
                                userId,
                                SEED_LIMIT
                        );

        if (seeds.isEmpty()) {

            return List.of();
        }

        /*
         * Tracks the user already liked or
         * recently played should normally not
         * appear as recommendations.
         */
        Set<UUID> knownTrackIds =
                new HashSet<>(
                        recommendationSignalRepository
                                .findKnownTrackIds(
                                        userId
                                )
                );

        Map<UUID, CandidateAccumulator>
                candidates =
                new HashMap<>();

        for (RecommendationSeed seed :
                seeds) {

            /*
             * We deliberately use pure semantic
             * search here instead of hybrid search.
             *
             * HybridTrackSearchService may resolve
             * an explicitly mentioned artist and
             * restrict the query to that artist.
             *
             * For recommendations we want discovery
             * across artists.
             */
            String semanticQuery =
                    buildSemanticQuery(
                            seed
                    );

            List<TrackSearchResult> results =
                    semanticTrackSearchService
                            .search(
                                    semanticQuery,
                                    CANDIDATES_PER_SEED
                            );

            for (int index = 0;
                    index < results.size();
                    index++) {

                TrackSearchResult result =
                        results.get(index);

                if (knownTrackIds.contains(
                        result.trackId())) {

                    continue;
                }

                /*
                 * Do not compare raw OpenSearch
                 * scores across separate searches.
                 *
                 * Scores from query A and query B
                 * are not necessarily calibrated
                 * against each other.
                 *
                 * Instead use rank within each
                 * seed search.
                 */
                long rankWeight =
                        CANDIDATES_PER_SEED
                                - index;

                long contribution =
                        seed.signalScore()
                                * rankWeight;

                CandidateAccumulator accumulator =
                        candidates.computeIfAbsent(
                                result.trackId(),
                                ignored ->
                                        new CandidateAccumulator(
                                                result.trackId()
                                        )
                        );

                accumulator.add(
                        contribution,
                        reason(seed)
                );
            }
        }

        List<RecommendationCandidate> ranked =
                new ArrayList<>();

        for (CandidateAccumulator accumulator :
                candidates.values()) {

            ranked.add(
                    accumulator.toCandidate()
            );
        }

        ranked.sort(
                Comparator
                        .comparingLong(
                                RecommendationCandidate::score
                        )
                        .reversed()
                        .thenComparing(
                                RecommendationCandidate::trackId
                        )
        );

        if (ranked.size() <= limit) {

            return List.copyOf(
                    ranked
            );
        }

        return List.copyOf(
                ranked.subList(
                        0,
                        limit
                )
        );
    }

    private String buildSemanticQuery(
            RecommendationSeed seed) {

        /*
         * This intentionally resembles the text
         * stored in the search index:
         *
         * Track
         * Artist
         * Album
         */
        return """
                Track: %s
                Artist: %s
                Album: %s
                """.formatted(
                seed.title(),
                seed.artistName(),
                seed.albumTitle()
        );
    }

    private String reason(
            RecommendationSeed seed) {

        if (seed.liked()) {

            return "Because you liked "
                    + seed.title();
        }

        if (seed.recentPlayCount() >= 3) {

            return "Because you recently played "
                    + seed.title()
                    + " several times";
        }

        return "Because you recently played "
                + seed.title();
    }

    private void validateLimit(
            int limit) {

        if (limit < 1
                || limit > MAX_RESULTS) {

            throw new IllegalArgumentException(
                    "Recommendation limit must be between 1 and "
                            + MAX_RESULTS
            );
        }
    }

    /*
     * A candidate can be discovered from
     * several different seed tracks.
     *
     * Example:
     *
     * Candidate X
     *   similar to Seed A -> +600
     *   similar to Seed B -> +350
     *
     * final score = 950
     *
     * That is a useful signal because several
     * parts of the user's taste profile point
     * toward the same track.
     */
    private static final class
            CandidateAccumulator {

        private final UUID trackId;

        private long totalScore;

        private long strongestContribution;

        private String strongestReason;

        private CandidateAccumulator(
                UUID trackId) {

            this.trackId =
                    trackId;
        }

        private void add(
                long contribution,
                String reason) {

            totalScore +=
                    contribution;

            if (contribution
                    > strongestContribution) {

                strongestContribution =
                        contribution;

                strongestReason =
                        reason;
            }
        }

        private RecommendationCandidate
                toCandidate() {

            return new RecommendationCandidate(
                    trackId,
                    totalScore,
                    strongestReason
            );
        }
    }
}
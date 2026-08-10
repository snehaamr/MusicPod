package com.musicpod.recommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;

@Service
public class RecommendationService {

    private static final int DEFAULT_SIZE =
            10;

    private static final int MAX_SIZE =
            50;

    private final RecommendationCandidateService
            recommendationCandidateService;

    private final TrackService trackService;

    public RecommendationService(
            RecommendationCandidateService
                    recommendationCandidateService,
            TrackService trackService) {

        this.recommendationCandidateService =
                recommendationCandidateService;

        this.trackService =
                trackService;
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse>
            getRecommendations(
                    UUID userId,
                    Integer size) {

        int effectiveSize =
                normalizeSize(size);

        List<RecommendationCandidate> candidates =
                recommendationCandidateService
                        .generateCandidates(
                                userId,
                                effectiveSize
                        );

        if (candidates.isEmpty()) {

            return List.of();
        }

        /*
         * Fetch all track metadata in one query.
         *
         * Do not call TrackService.getById()
         * once per recommendation.
         */
        List<UUID> trackIds =
                candidates
                        .stream()
                        .map(
                                RecommendationCandidate::trackId
                        )
                        .toList();

        Map<UUID, TrackResponse> tracksById =
                trackService.getByIds(
                        trackIds
                );

        /*
         * The candidate service owns ranking.
         *
         * Database IN (...) ordering is not
         * guaranteed, so rebuild the response
         * using candidate order.
         */
        List<RecommendationResponse> result =
                new ArrayList<>(
                        candidates.size()
                );

        for (RecommendationCandidate candidate :
                candidates) {

            TrackResponse track =
                    tracksById.get(
                            candidate.trackId()
                    );

            if (track == null) {

                continue;
            }

            int rank =
                    result.size() + 1;

            result.add(
                    new RecommendationResponse(
                            rank,
                            track,
                            candidate.score(),
                            candidate.reason()
                    )
            );
        }

        return List.copyOf(
                result
        );
    }

    private int normalizeSize(
            Integer size) {

        if (size == null) {

            return DEFAULT_SIZE;
        }

        if (size < 1
                || size > MAX_SIZE) {

            throw new IllegalArgumentException(
                    "Recommendation size must be between 1 and "
                            + MAX_SIZE
            );
        }

        return size;
    }
}
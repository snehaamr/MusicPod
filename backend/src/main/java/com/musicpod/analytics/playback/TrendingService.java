package com.musicpod.analytics.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;

@Service
public class TrendingService {

    private static final int DEFAULT_SIZE =
            20;

    private static final int MAX_SIZE =
            50;

    private final TrackPlayHourlyStatsRepository
            trackPlayHourlyStatsRepository;

    private final TrackService
            trackService;

    public TrendingService(
            TrackPlayHourlyStatsRepository
                    trackPlayHourlyStatsRepository,
            TrackService trackService) {

        this.trackPlayHourlyStatsRepository =
                trackPlayHourlyStatsRepository;

        this.trackService =
                trackService;
    }

    @Transactional(readOnly = true)
    public List<TrendingTrackResponse>
            getTrendingTracks(
                    Integer size) {

        int effectiveSize =
                normalizeSize(
                        size
                );

        /*
         * Query the analytics read model first.
         *
         * This list is already ordered by:
         *
         * trendingScore DESC
         * currentHourPlays DESC
         * playsLast24Hours DESC
         * ...
         */
        List<TrendingTrackStats> trendingStats =
                trackPlayHourlyStatsRepository
                        .findTrending(
                                effectiveSize
                        );

        if (trendingStats.isEmpty()) {

            return List.of();
        }

        /*
         * Extract all ranked track IDs.
         *
         * We intentionally do not call:
         *
         * trackService.getById(...)
         *
         * inside the loop.
         *
         * That would create an N+1 lookup.
         */
        List<UUID> trackIds =
                trendingStats
                        .stream()
                        .map(
                                TrendingTrackStats::trackId
                        )
                        .toList();

        /*
         * One batch catalog lookup.
         */
        Map<UUID, TrackResponse> tracksById =
                trackService
                        .getByIds(
                                trackIds
                        );

        /*
         * Batch database queries do not
         * guarantee the same ordering as
         * the IN (...) input.
         *
         * Therefore the analytics result
         * remains the authority for rank.
         */
        List<TrendingTrackResponse> response =
                new ArrayList<>(
                        trendingStats.size()
                );

        for (TrendingTrackStats stats :
                trendingStats) {

            TrackResponse track =
                    tracksById.get(
                            stats.trackId()
                    );

            /*
             * Normally impossible because
             * analytics rows reference tracks
             * using ON DELETE CASCADE.
             *
             * Still, tolerate a track disappearing
             * between the analytics query and
             * catalog lookup.
             */
            if (track == null) {

                continue;
            }

            int rank =
                    response.size() + 1;

            response.add(
                    new TrendingTrackResponse(
                            rank,
                            track,
                            stats.currentHourPlays(),
                            stats.playsLast24Hours(),
                            stats.playsLast7Days(),
                            stats.playedMsLast7Days(),
                            stats.trendingScore()
                    )
            );
        }

        return List.copyOf(
                response
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
                    "Trending size must be between 1 and "
                            + MAX_SIZE
            );
        }

        return size;
    }
}
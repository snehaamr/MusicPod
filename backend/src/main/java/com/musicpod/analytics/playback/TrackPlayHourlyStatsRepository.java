package com.musicpod.analytics.playback;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

@Repository
public class TrackPlayHourlyStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public TrackPlayHourlyStatsRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    public void increment(
            PlaybackRecordedEvent event) {

        /*
         * PlaybackRecordedEvent.playedAt()
         * is an Instant.
         *
         * Instant truncation therefore gives us
         * a stable UTC hour boundary.
         *
         * Example:
         *
         * 2026-08-10T18:37:42Z
         *
         * becomes:
         *
         * 2026-08-10T18:00:00Z
         */
        Instant bucketStart =
                event
                        .playedAt()
                        .truncatedTo(
                                ChronoUnit.HOURS
                        );

        jdbcTemplate.update(
                """
                INSERT INTO track_play_stats_hourly (
                    track_id,
                    bucket_start,
                    play_count,
                    total_played_ms
                )
                VALUES (
                    ?,
                    ?,
                    1,
                    ?
                )
                ON CONFLICT (
                    track_id,
                    bucket_start
                )
                DO UPDATE SET

                    play_count =
                        track_play_stats_hourly.play_count
                        + 1,

                    total_played_ms =
                        track_play_stats_hourly.total_played_ms
                        + EXCLUDED.total_played_ms
                """,
                event.trackId(),

                Timestamp.from(
                        bucketStart
                ),

                (long) event.playedMs()
        );
    }

    public void deleteAll() {

        jdbcTemplate.update(
                "DELETE FROM track_play_stats_hourly"
        );
    }
    
    public List<TrendingTrackStats> findTrending(
            int limit) {

        return jdbcTemplate.query(
                """
                WITH bounds AS (
                    SELECT
                        date_trunc(
                            'hour',
                            CURRENT_TIMESTAMP
                        ) AS current_hour
                ),

                aggregated AS (
                    SELECT
                        stats.track_id,

                        /*
                         * Current partial UTC hour.
                         */
                        COALESCE(
                            SUM(stats.play_count)
                                FILTER (
                                    WHERE stats.bucket_start
                                        >= bounds.current_hour
                                ),
                            0
                        ) AS current_hour_plays,

                        /*
                         * Current hour + previous
                         * 23 complete hourly buckets.
                         */
                        COALESCE(
                            SUM(stats.play_count)
                                FILTER (
                                    WHERE stats.bucket_start
                                        > bounds.current_hour
                                            - INTERVAL '24 hours'
                                ),
                            0
                        ) AS plays_last_24_hours,

                        /*
                         * Current hour + previous
                         * 167 hourly buckets.
                         */
                        COALESCE(
                            SUM(stats.play_count)
                                FILTER (
                                    WHERE stats.bucket_start
                                        > bounds.current_hour
                                            - INTERVAL '7 days'
                                ),
                            0
                        ) AS plays_last_7_days,

                        COALESCE(
                            SUM(stats.total_played_ms)
                                FILTER (
                                    WHERE stats.bucket_start
                                        > bounds.current_hour
                                            - INTERVAL '7 days'
                                ),
                            0
                        ) AS played_ms_last_7_days,

                        /*
                         * Trending score.
                         *
                         * Windows are intentionally
                         * non-overlapping.
                         *
                         * current hour   -> weight 10
                         * previous 23h   -> weight 3
                         * previous 6d    -> weight 1
                         */
                        COALESCE(
                            SUM(
                                CASE

                                    WHEN stats.bucket_start
                                        >= bounds.current_hour
                                    THEN stats.play_count * 10

                                    WHEN stats.bucket_start
                                        > bounds.current_hour
                                            - INTERVAL '24 hours'
                                    THEN stats.play_count * 3

                                    WHEN stats.bucket_start
                                        > bounds.current_hour
                                            - INTERVAL '7 days'
                                    THEN stats.play_count

                                    ELSE 0

                                END
                            ),
                            0
                        ) AS trending_score

                    FROM track_play_stats_hourly stats

                    CROSS JOIN bounds

                    /*
                     * Do not scan analytics history
                     * that cannot affect trending.
                     */
                    WHERE stats.bucket_start
                        > bounds.current_hour
                            - INTERVAL '7 days'

                    GROUP BY
                        stats.track_id
                )

                SELECT
                    track_id,
                    current_hour_plays,
                    plays_last_24_hours,
                    plays_last_7_days,
                    played_ms_last_7_days,
                    trending_score

                FROM aggregated

                ORDER BY
                    trending_score DESC,

                    /*
                     * When scores tie, prefer the
                     * track with stronger immediate
                     * momentum.
                     */
                    current_hour_plays DESC,

                    plays_last_24_hours DESC,

                    /*
                     * Then prefer actual listening
                     * duration rather than an
                     * arbitrary UUID ordering.
                     */
                    played_ms_last_7_days DESC,

                    /*
                     * Deterministic final ordering.
                     */
                    track_id ASC

                LIMIT ?
                """,

                (resultSet, rowNumber) ->
                        new TrendingTrackStats(
                                resultSet.getObject(
                                        "track_id",
                                        UUID.class
                                ),
                                resultSet.getLong(
                                        "current_hour_plays"
                                ),
                                resultSet.getLong(
                                        "plays_last_24_hours"
                                ),
                                resultSet.getLong(
                                        "plays_last_7_days"
                                ),
                                resultSet.getLong(
                                        "played_ms_last_7_days"
                                ),
                                resultSet.getLong(
                                        "trending_score"
                                )
                        ),

                limit
        );
    }
}
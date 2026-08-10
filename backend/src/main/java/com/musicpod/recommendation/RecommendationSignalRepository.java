package com.musicpod.recommendation;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecommendationSignalRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationSignalRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    public List<RecommendationSeed> findTopSeeds(
            UUID userId,
            int limit) {

        return jdbcTemplate.query(
                """
                WITH liked AS (

                    SELECT
                        liked_track.track_id

                    FROM liked_tracks liked_track

                    WHERE liked_track.user_id = ?
                ),

                recent_plays AS (

                    SELECT
                        playback.track_id,

                        COUNT(*) AS play_count,

                        MAX(
                            playback.played_at
                        ) AS last_played_at

                    FROM playback_events playback

                    WHERE playback.user_id = ?

                      AND playback.played_at
                            >= CURRENT_TIMESTAMP
                               - INTERVAL '30 days'

                    GROUP BY
                        playback.track_id
                ),

                signals AS (

                    SELECT

                        COALESCE(
                            liked.track_id,
                            recent.track_id
                        ) AS track_id,

                        liked.track_id IS NOT NULL
                            AS liked,

                        COALESCE(
                            recent.play_count,
                            0
                        ) AS play_count,

                        recent.last_played_at
                            AS last_played_at

                    FROM liked

                    FULL OUTER JOIN recent_plays recent
                        ON recent.track_id =
                           liked.track_id
                )

                SELECT

                    track.id AS track_id,

                    track.title AS title,

                    album.id AS album_id,

                    album.title AS album_title,

                    artist.id AS artist_id,

                    artist.name AS artist_name,

                    signals.liked,

                    signals.play_count,

                    signals.last_played_at,

                    (
                        /*
                         * Explicit like is the strongest
                         * user signal.
                         */
                        CASE
                            WHEN signals.liked
                                THEN 50
                            ELSE 0
                        END

                        +

                        /*
                         * Repeat listening matters,
                         * but cap it so a single song
                         * cannot dominate forever.
                         */
                        LEAST(
                            signals.play_count,
                            10
                        ) * 5

                        +

                        /*
                         * Recency bonus.
                         */
                        CASE

                            WHEN signals.last_played_at
                                >= CURRENT_TIMESTAMP
                                   - INTERVAL '24 hours'
                            THEN 20

                            WHEN signals.last_played_at
                                >= CURRENT_TIMESTAMP
                                   - INTERVAL '7 days'
                            THEN 10

                            WHEN signals.last_played_at
                                IS NOT NULL
                            THEN 5

                            ELSE 0

                        END

                    ) AS signal_score

                FROM signals

                JOIN tracks track
                    ON track.id =
                       signals.track_id

                JOIN albums album
                    ON album.id =
                       track.album_id

                JOIN artists artist
                    ON artist.id =
                       album.artist_id

                ORDER BY

                    signal_score DESC,

                    signals.last_played_at
                        DESC NULLS LAST,

                    track.id ASC

                LIMIT ?
                """,

                (resultSet, rowNumber) ->
                        new RecommendationSeed(

                                resultSet.getObject(
                                        "track_id",
                                        UUID.class
                                ),

                                resultSet.getString(
                                        "title"
                                ),

                                resultSet.getObject(
                                        "album_id",
                                        UUID.class
                                ),

                                resultSet.getString(
                                        "album_title"
                                ),

                                resultSet.getObject(
                                        "artist_id",
                                        UUID.class
                                ),

                                resultSet.getString(
                                        "artist_name"
                                ),

                                resultSet.getBoolean(
                                        "liked"
                                ),

                                resultSet.getLong(
                                        "play_count"
                                ),

                                resultSet.getTimestamp(
                                                "last_played_at"
                                        ) == null
                                        ? null
                                        : resultSet
                                                .getTimestamp(
                                                        "last_played_at"
                                                )
                                                .toInstant(),

                                resultSet.getLong(
                                        "signal_score"
                                )
                        ),

                userId,
                userId,
                limit
        );
    }
    
    public List<UUID> findKnownTrackIds(
            UUID userId) {

        return jdbcTemplate.query(
                """
                SELECT DISTINCT track_id

                FROM (

                    SELECT
                        track_id

                    FROM liked_tracks

                    WHERE user_id = ?

                    UNION

                    SELECT
                        track_id

                    FROM playback_events

                    WHERE user_id = ?

                      AND played_at
                            >= CURRENT_TIMESTAMP
                               - INTERVAL '30 days'

                ) known_tracks
                """,

                (resultSet, rowNumber) ->
                        resultSet.getObject(
                                "track_id",
                                UUID.class
                        ),

                userId,
                userId
        );
    }
}
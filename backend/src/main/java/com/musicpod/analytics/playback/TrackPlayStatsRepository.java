package com.musicpod.analytics.playback;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

@Repository
public class TrackPlayStatsRepository {

    private final JdbcTemplate jdbcTemplate;

    public TrackPlayStatsRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    public void increment(
            PlaybackRecordedEvent event) {

        jdbcTemplate.update(
                """
                INSERT INTO track_play_stats (
                    track_id,
                    play_count,
                    total_played_ms,
                    last_played_at
                )
                VALUES (?, 1, ?, ?)
                ON CONFLICT (track_id)
                DO UPDATE SET
                    play_count =
                        track_play_stats.play_count + 1,

                    total_played_ms =
                        track_play_stats.total_played_ms
                        + EXCLUDED.total_played_ms,

                    last_played_at =
                        GREATEST(
                            track_play_stats.last_played_at,
                            EXCLUDED.last_played_at
                        )
                """,
                event.trackId(),
                (long) event.playedMs(),
                Timestamp.from(
                        event.playedAt()
                )
        );
    }

    public Optional<TrackPlayStats>
            findByTrackId(
                    UUID trackId) {

        List<TrackPlayStats> rows =
                jdbcTemplate.query(
                        """
                        SELECT
                            track_id,
                            play_count,
                            total_played_ms,
                            last_played_at
                        FROM track_play_stats
                        WHERE track_id = ?
                        """,
                        (resultSet, rowNumber) ->
                                new TrackPlayStats(
                                        resultSet.getObject(
                                                "track_id",
                                                UUID.class
                                        ),
                                        resultSet.getLong(
                                                "play_count"
                                        ),
                                        resultSet.getLong(
                                                "total_played_ms"
                                        ),
                                        resultSet.getTimestamp(
                                                "last_played_at"
                                        )
                                                .toInstant()
                                ),
                        trackId
                );

        return rows
                .stream()
                .findFirst();
    }

    public void deleteAll() {

        jdbcTemplate.update(
                "DELETE FROM track_play_stats"
        );
    }
}
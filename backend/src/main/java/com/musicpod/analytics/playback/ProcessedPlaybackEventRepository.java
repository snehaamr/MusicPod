package com.musicpod.analytics.playback;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProcessedPlaybackEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedPlaybackEventRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    public boolean markProcessedIfAbsent(
            UUID eventId) {

        int inserted =
                jdbcTemplate.update(
                        """
                        INSERT INTO processed_playback_events (
                            event_id,
                            processed_at
                        )
                        VALUES (
                            ?,
                            CURRENT_TIMESTAMP
                        )
                        ON CONFLICT (event_id)
                        DO NOTHING
                        """,
                        eventId
                );

        return inserted == 1;
    }

    public void deleteAll() {

        jdbcTemplate.update(
                "DELETE FROM processed_playback_events"
        );
    }
}
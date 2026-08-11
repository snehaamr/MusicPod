package com.musicpod.messaging.outbox;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxEventRepository {

	private static final String CLAIM_SQL = """
	        WITH candidates AS (
	            SELECT current_event.id
	            FROM outbox_events current_event
	            WHERE
	                (
	                    (
	                        current_event.status = 'PENDING'
	                        AND current_event.available_at <= CURRENT_TIMESTAMP
	                    )
	                    OR
	                    (
	                        current_event.status = 'PROCESSING'
	                        AND current_event.locked_at <
	                            CURRENT_TIMESTAMP - INTERVAL '15 minutes'
	                    )
	                )
	                AND NOT EXISTS (
	                    SELECT 1
	                    FROM outbox_events older_event
	                    WHERE older_event.topic = current_event.topic
	                      AND older_event.message_key =
	                          current_event.message_key
	                      AND older_event.status IN (
	                          'PENDING',
	                          'PROCESSING'
	                      )
	                      AND (
	                          older_event.created_at <
	                              current_event.created_at
	                          OR (
	                              older_event.created_at =
	                                  current_event.created_at
	                              AND older_event.id <
	                                  current_event.id
	                          )
	                      )
	                )
	            ORDER BY
	                current_event.created_at,
	                current_event.id
	            FOR UPDATE SKIP LOCKED
	            LIMIT ?
	        )
	        UPDATE outbox_events o
	        SET
	            status = 'PROCESSING',
	            locked_at = CURRENT_TIMESTAMP,
	            attempts = o.attempts + 1
	        FROM candidates c
	        WHERE o.id = c.id
	        RETURNING
	            o.id,
	            o.aggregate_type,
	            o.aggregate_id,
	            o.event_type,
	            o.topic,
	            o.message_key,
	            o.payload::text AS payload,
	            o.attempts,
	            o.created_at
	        """;

    private static final RowMapper<OutboxEvent>
            OUTBOX_EVENT_ROW_MAPPER =
            (rs, rowNum) -> new OutboxEvent(
                    rs.getObject(
                            "id",
                            UUID.class
                    ),
                    rs.getString(
                            "aggregate_type"
                    ),
                    rs.getObject(
                            "aggregate_id",
                            UUID.class
                    ),
                    rs.getString(
                            "event_type"
                    ),
                    rs.getString(
                            "topic"
                    ),
                    rs.getString(
                            "message_key"
                    ),
                    rs.getString(
                            "payload"
                    ),
                    rs.getInt(
                            "attempts"
                    ),
                    rs.getTimestamp(
                            "created_at"
                    ).toInstant()
            );

    private final JdbcTemplate jdbcTemplate;

    public OutboxEventRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate =
                jdbcTemplate;
    }

    public void insert(
            UUID id,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String messageKey,
            String payload) {

        jdbcTemplate.update(
                """
                INSERT INTO outbox_events (
                    id,
                    aggregate_type,
                    aggregate_id,
                    event_type,
                    topic,
                    message_key,
                    payload,
                    status,
                    attempts,
                    available_at,
                    created_at
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    CAST(? AS jsonb),
                    'PENDING',
                    0,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                id,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                messageKey,
                payload
        );
    }

    public List<OutboxEvent> claimBatch(
            int batchSize) {

        return jdbcTemplate.query(
                CLAIM_SQL,
                preparedStatement ->
                        preparedStatement.setInt(
                                1,
                                batchSize
                        ),
                OUTBOX_EVENT_ROW_MAPPER
        );
    }

    public void markPublished(
            UUID id) {

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET
                    status = 'PUBLISHED',
                    published_at = CURRENT_TIMESTAMP,
                    locked_at = NULL,
                    last_error = NULL
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                id
        );
    }

    public void markFailed(
            UUID id,
            Instant availableAt,
            String error) {

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET
                    status = 'PENDING',
                    available_at = ?,
                    locked_at = NULL,
                    last_error = ?
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                Timestamp.from(
                        availableAt
                ),
                error,
                id
        );
    }

    public void markDead(
            UUID id,
            String error) {

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET
                    status = 'DEAD',
                    locked_at = NULL,
                    last_error = ?
                WHERE id = ?
                  AND status = 'PROCESSING'
                """,
                error,
                id
        );
    }
    
    public long countByStatus(
            String status) {

        Long count =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM outbox_events
                        WHERE status = ?
                        """,
                        Long.class,
                        status
                );

        return count == null
                ? 0L
                : count;
    }
}
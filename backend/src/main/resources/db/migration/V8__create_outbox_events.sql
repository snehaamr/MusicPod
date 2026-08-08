CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,

    event_type VARCHAR(200) NOT NULL,

    topic VARCHAR(255) NOT NULL,
    message_key VARCHAR(255) NOT NULL,

    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    attempts INTEGER NOT NULL DEFAULT 0,

    available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,

    last_error TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT chk_outbox_status
        CHECK (
            status IN (
                'PENDING',
                'PROCESSING',
                'PUBLISHED',
                'DEAD'
            )
        ),

    CONSTRAINT chk_outbox_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_dispatch
    ON outbox_events(
        status,
        available_at,
        created_at
    )
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_outbox_aggregate
    ON outbox_events(
        aggregate_type,
        aggregate_id,
        created_at
    );
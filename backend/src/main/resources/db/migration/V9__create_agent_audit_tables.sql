CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,

    prompt VARCHAR(1000) NOT NULL,
    allow_write BOOLEAN NOT NULL DEFAULT FALSE,

    status VARCHAR(32) NOT NULL,

    response TEXT,
    error_message TEXT,

    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT ck_agent_runs_status
        CHECK (status IN (
            'RUNNING',
            'COMPLETED',
            'FAILED'
        ))
);

CREATE INDEX idx_agent_runs_user_started
    ON agent_runs (
        user_id,
        started_at DESC
    );

CREATE INDEX idx_agent_runs_status
    ON agent_runs (status);


CREATE TABLE agent_steps (
    id UUID PRIMARY KEY,

    run_id UUID NOT NULL,

    sequence_number INTEGER NOT NULL,

    tool_name VARCHAR(150) NOT NULL,
    tool_risk VARCHAR(32) NOT NULL,

    input_json TEXT,
    output_json TEXT,

    status VARCHAR(32) NOT NULL,

    duration_ms BIGINT,

    error_message TEXT,

    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_agent_steps_run
        FOREIGN KEY (run_id)
        REFERENCES agent_runs(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_agent_steps_run_sequence
        UNIQUE (
            run_id,
            sequence_number
        ),

    CONSTRAINT ck_agent_steps_status
        CHECK (status IN (
            'RUNNING',
            'COMPLETED',
            'FAILED'
        )),

    CONSTRAINT ck_agent_steps_duration
        CHECK (
            duration_ms IS NULL
            OR duration_ms >= 0
        )
);

CREATE INDEX idx_agent_steps_run
    ON agent_steps (
        run_id,
        sequence_number
    );

CREATE INDEX idx_agent_steps_tool
    ON agent_steps (tool_name);
CREATE TABLE track_play_stats_hourly (
    track_id UUID NOT NULL,

    bucket_start TIMESTAMP WITH TIME ZONE NOT NULL,

    play_count BIGINT NOT NULL,

    total_played_ms BIGINT NOT NULL,

    CONSTRAINT pk_track_play_stats_hourly
        PRIMARY KEY (
            track_id,
            bucket_start
        ),

    CONSTRAINT fk_track_play_stats_hourly_track
        FOREIGN KEY (track_id)
        REFERENCES tracks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_track_play_stats_hourly_play_count
        CHECK (play_count >= 0),

    CONSTRAINT chk_track_play_stats_hourly_total_played_ms
        CHECK (total_played_ms >= 0)
);


CREATE INDEX idx_track_play_stats_hourly_bucket_start
    ON track_play_stats_hourly (
        bucket_start
    );
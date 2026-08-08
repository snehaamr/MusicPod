CREATE TABLE processed_playback_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);


CREATE TABLE track_play_stats (
    track_id UUID PRIMARY KEY,
    play_count BIGINT NOT NULL,
    total_played_ms BIGINT NOT NULL,
    last_played_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_track_play_stats_track
        FOREIGN KEY (track_id)
        REFERENCES tracks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_track_play_stats_play_count
        CHECK (play_count >= 0),

    CONSTRAINT chk_track_play_stats_total_played_ms
        CHECK (total_played_ms >= 0)
);
CREATE TABLE playback_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    track_id UUID NOT NULL,
    played_ms INTEGER NOT NULL,
    played_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_playback_events_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_playback_events_track
        FOREIGN KEY (track_id)
        REFERENCES tracks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_playback_events_played_ms
        CHECK (played_ms > 0)
);

CREATE INDEX idx_playback_events_user_played_at
    ON playback_events(
        user_id,
        played_at DESC,
        id DESC
    );

CREATE INDEX idx_playback_events_user_track_played_at
    ON playback_events(
        user_id,
        track_id,
        played_at DESC
    );

CREATE INDEX idx_playback_events_track_played_at
    ON playback_events(
        track_id,
        played_at DESC
    );
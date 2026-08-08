CREATE TABLE liked_tracks (
    user_id UUID NOT NULL,
    track_id UUID NOT NULL,
    liked_at TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY (user_id, track_id),

    CONSTRAINT fk_liked_tracks_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_liked_tracks_track
        FOREIGN KEY (track_id)
        REFERENCES tracks(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_liked_tracks_user_liked_at
    ON liked_tracks(user_id, liked_at DESC, track_id);

CREATE INDEX idx_liked_tracks_track_id
    ON liked_tracks(track_id);
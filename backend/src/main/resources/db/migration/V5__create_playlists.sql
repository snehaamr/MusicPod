CREATE TABLE playlists (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_playlists_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_playlists_user_updated
    ON playlists(user_id, updated_at DESC, id);


CREATE TABLE playlist_tracks (
    playlist_id UUID NOT NULL,
    track_id UUID NOT NULL,
    position INTEGER NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY (playlist_id, track_id),

    CONSTRAINT fk_playlist_tracks_playlist
        FOREIGN KEY (playlist_id)
        REFERENCES playlists(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_playlist_tracks_track
        FOREIGN KEY (track_id)
        REFERENCES tracks(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_playlist_track_position
        CHECK (position > 0),

    CONSTRAINT uq_playlist_track_position
        UNIQUE (playlist_id, position)
);

CREATE INDEX idx_playlist_tracks_playlist_position
    ON playlist_tracks(playlist_id, position);

CREATE INDEX idx_playlist_tracks_track_id
    ON playlist_tracks(track_id);
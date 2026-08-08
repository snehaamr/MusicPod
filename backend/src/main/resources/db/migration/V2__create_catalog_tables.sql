CREATE TABLE artists (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    bio TEXT,
    image_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE albums (
    id UUID PRIMARY KEY,
    artist_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    release_date DATE,
    cover_image_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_albums_artist
        FOREIGN KEY (artist_id)
        REFERENCES artists(id)
);

CREATE TABLE tracks (
    id UUID PRIMARY KEY,
    album_id UUID NOT NULL,
    title VARCHAR(300) NOT NULL,
    track_number INTEGER NOT NULL,
    duration_ms INTEGER NOT NULL,
    explicit BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_tracks_album
        FOREIGN KEY (album_id)
        REFERENCES albums(id),

    CONSTRAINT chk_tracks_duration
        CHECK (duration_ms > 0),

    CONSTRAINT chk_tracks_track_number
        CHECK (track_number > 0)
);

CREATE INDEX idx_albums_artist_id
    ON albums(artist_id);

CREATE INDEX idx_tracks_album_id
    ON tracks(album_id);

CREATE UNIQUE INDEX uq_track_number_per_album
    ON tracks(album_id, track_number);
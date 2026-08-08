package com.musicpod.library.playlist;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PlaylistTrackId
        implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "playlist_id")
    private UUID playlistId;

    @Column(name = "track_id")
    private UUID trackId;

    protected PlaylistTrackId() {
        // Required by JPA.
    }

    public PlaylistTrackId(
            UUID playlistId,
            UUID trackId) {

        this.playlistId = playlistId;
        this.trackId = trackId;
    }

    public UUID getPlaylistId() {
        return playlistId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof PlaylistTrackId other)) {
            return false;
        }

        return Objects.equals(
                playlistId,
                other.playlistId
        )
                && Objects.equals(
                        trackId,
                        other.trackId
                );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                playlistId,
                trackId
        );
    }
}
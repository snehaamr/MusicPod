package com.musicpod.library.likedtrack;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LikedTrackId
        implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "track_id")
    private UUID trackId;

    protected LikedTrackId() {
        // Required by JPA.
    }

    public LikedTrackId(
            UUID userId,
            UUID trackId) {

        this.userId = userId;
        this.trackId = trackId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTrackId() {
        return trackId;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof LikedTrackId other)) {
            return false;
        }

        return Objects.equals(
                userId,
                other.userId
        )
                && Objects.equals(
                        trackId,
                        other.trackId
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                userId,
                trackId
        );
    }
}
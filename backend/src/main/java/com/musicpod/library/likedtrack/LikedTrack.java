package com.musicpod.library.likedtrack;

import java.time.Instant;

import com.musicpod.catalog.track.Track;
import com.musicpod.user.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "liked_tracks")
public class LikedTrack {

    @EmbeddedId
    private LikedTrackId id;

    @MapsId("userId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private UserAccount user;

    @MapsId("trackId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "track_id",
            nullable = false
    )
    private Track track;

    @Column(
            name = "liked_at",
            nullable = false
    )
    private Instant likedAt;

    protected LikedTrack() {
        // Required by JPA.
    }

    public LikedTrack(
            UserAccount user,
            Track track) {

        this.user = user;
        this.track = track;

        this.id = new LikedTrackId(
                user.getId(),
                track.getId()
        );
    }

    @PrePersist
    void onCreate() {

        if (likedAt == null) {
            likedAt = Instant.now();
        }
    }

    public LikedTrackId getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public Track getTrack() {
        return track;
    }

    public Instant getLikedAt() {
        return likedAt;
    }
}
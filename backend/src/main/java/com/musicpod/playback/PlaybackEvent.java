package com.musicpod.playback;

import java.time.Instant;
import java.util.UUID;

import com.musicpod.catalog.track.Track;
import com.musicpod.user.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "playback_events")
public class PlaybackEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private UserAccount user;

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
            name = "played_ms",
            nullable = false
    )
    private int playedMs;

    @Column(
            name = "played_at",
            nullable = false
    )
    private Instant playedAt;

    protected PlaybackEvent() {
        // Required by JPA.
    }

    public PlaybackEvent(
            UserAccount user,
            Track track,
            int playedMs) {

        this.user = user;
        this.track = track;
        this.playedMs = playedMs;
    }

    @PrePersist
    void onCreate() {

        if (playedAt == null) {
            playedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public Track getTrack() {
        return track;
    }

    public int getPlayedMs() {
        return playedMs;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }
}
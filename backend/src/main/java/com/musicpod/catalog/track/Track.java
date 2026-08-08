package com.musicpod.catalog.track;

import java.time.Instant;
import java.util.UUID;

import com.musicpod.catalog.album.Album;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tracks")
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "album_id",
            nullable = false
    )
    private Album album;

    @Column(
            nullable = false,
            length = 300
    )
    private String title;

    @Column(
            name = "track_number",
            nullable = false
    )
    private int trackNumber;

    @Column(
            name = "duration_ms",
            nullable = false
    )
    private int durationMs;

    @Column(
            name = "explicit",
            nullable = false
    )
    private boolean explicit;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    protected Track() {
        // Required by JPA.
    }

    public Track(
            Album album,
            String title,
            int trackNumber,
            int durationMs,
            boolean explicit) {

        this.album = album;
        this.title = title;
        this.trackNumber = trackNumber;
        this.durationMs = durationMs;
        this.explicit = explicit;
    }

    @PrePersist
    void onCreate() {

        Instant now = Instant.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(
            String title,
            int trackNumber,
            int durationMs,
            boolean explicit) {

        this.title = title;
        this.trackNumber = trackNumber;
        this.durationMs = durationMs;
        this.explicit = explicit;
    }

    public UUID getId() {
        return id;
    }

    public Album getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public int getTrackNumber() {
        return trackNumber;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
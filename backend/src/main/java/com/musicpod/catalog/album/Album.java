package com.musicpod.catalog.album;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.musicpod.catalog.artist.Artist;

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
@Table(name = "albums")
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "artist_id",
            nullable = false
    )
    private Artist artist;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(
            name = "cover_image_url",
            length = 1000
    )
    private String coverImageUrl;

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

    protected Album() {
        // Required by JPA.
    }

    public Album(
            Artist artist,
            String title,
            LocalDate releaseDate,
            String coverImageUrl) {

        this.artist = artist;
        this.title = title;
        this.releaseDate = releaseDate;
        this.coverImageUrl = coverImageUrl;
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
            LocalDate releaseDate,
            String coverImageUrl) {

        this.title = title;
        this.releaseDate = releaseDate;
        this.coverImageUrl = coverImageUrl;
    }

    public UUID getId() {
        return id;
    }

    public Artist getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
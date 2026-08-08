package com.musicpod.library.playlist;

import java.time.Instant;

import com.musicpod.catalog.track.Track;

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
@Table(name = "playlist_tracks")
public class PlaylistTrack {

    @EmbeddedId
    private PlaylistTrackId id;

    @MapsId("playlistId")
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "playlist_id",
            nullable = false
    )
    private Playlist playlist;

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
            nullable = false
    )
    private int position;

    @Column(
            name = "added_at",
            nullable = false
    )
    private Instant addedAt;

    protected PlaylistTrack() {
        // Required by JPA.
    }

    public PlaylistTrack(
            Playlist playlist,
            Track track,
            int position) {

        this.playlist = playlist;
        this.track = track;
        this.position = position;

        this.id = new PlaylistTrackId(
                playlist.getId(),
                track.getId()
        );
    }

    @PrePersist
    void onCreate() {

        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }

    public PlaylistTrackId getId() {
        return id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public Track getTrack() {
        return track;
    }

    public int getPosition() {
        return position;
    }

    public Instant getAddedAt() {
        return addedAt;
    }
}
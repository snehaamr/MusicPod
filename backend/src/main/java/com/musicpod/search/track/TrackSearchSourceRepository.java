package com.musicpod.search.track;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.musicpod.catalog.track.Track;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class TrackSearchSourceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Track> findAllForSearch() {

        return entityManager
                .createQuery(
                        """
                        SELECT track
                        FROM Track track
                        JOIN FETCH track.album album
                        JOIN FETCH album.artist artist
                        ORDER BY track.id
                        """,
                        Track.class
                )
                .getResultList();
    }

    public List<Track> findAllForSearchByAlbumId(
            UUID albumId) {

        return entityManager
                .createQuery(
                        """
                        SELECT track
                        FROM Track track
                        JOIN FETCH track.album album
                        JOIN FETCH album.artist artist
                        WHERE album.id = :albumId
                        ORDER BY track.id
                        """,
                        Track.class
                )
                .setParameter(
                        "albumId",
                        albumId
                )
                .getResultList();
    }

    public List<Track> findAllForSearchByArtistId(
            UUID artistId) {

        return entityManager
                .createQuery(
                        """
                        SELECT track
                        FROM Track track
                        JOIN FETCH track.album album
                        JOIN FETCH album.artist artist
                        WHERE artist.id = :artistId
                        ORDER BY track.id
                        """,
                        Track.class
                )
                .setParameter(
                        "artistId",
                        artistId
                )
                .getResultList();
    }
}
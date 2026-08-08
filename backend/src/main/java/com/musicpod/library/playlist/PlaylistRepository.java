package com.musicpod.library.playlist;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PlaylistRepository
        extends JpaRepository<Playlist, UUID> {

    Page<Playlist> findByUser_Id(
            UUID userId,
            Pageable pageable
    );

    Optional<Playlist> findByIdAndUser_Id(
            UUID playlistId,
            UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT playlist
            FROM Playlist playlist
            WHERE playlist.id = :playlistId
              AND playlist.user.id = :userId
            """)
    Optional<Playlist> findOwnedPlaylistForUpdate(
            @Param("playlistId")
            UUID playlistId,

            @Param("userId")
            UUID userId
    );
}
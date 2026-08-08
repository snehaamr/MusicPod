package com.musicpod.catalog.album;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumRepository
        extends JpaRepository<Album, UUID> {

    Page<Album> findByArtist_Id(
            UUID artistId,
            Pageable pageable
    );
}
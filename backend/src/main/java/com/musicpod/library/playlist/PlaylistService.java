package com.musicpod.library.playlist;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.catalog.track.Track;
import com.musicpod.catalog.track.TrackRepository;
import com.musicpod.common.api.PageResponse;
import com.musicpod.common.exception.ResourceNotFoundException;
import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;

@Service
public class PlaylistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public PlaylistService(
            PlaylistRepository playlistRepository,
            PlaylistTrackRepository playlistTrackRepository,
            TrackRepository trackRepository,
            UserRepository userRepository) {

        this.playlistRepository =
                playlistRepository;

        this.playlistTrackRepository =
                playlistTrackRepository;

        this.trackRepository =
                trackRepository;

        this.userRepository =
                userRepository;
    }

    @Transactional
    public PlaylistResponse create(
            UUID userId,
            CreatePlaylistRequest request) {

        UserAccount user =
                findUser(userId);

        Playlist playlist =
                new Playlist(
                        user,
                        request.name(),
                        normalizeDescription(
                                request.description()
                        )
                );

        Playlist savedPlaylist =
                playlistRepository.save(
                        playlist
                );

        return PlaylistResponse.from(
                savedPlaylist
        );
    }

    @Transactional(readOnly = true)
    public PlaylistResponse getById(
            UUID userId,
            UUID playlistId) {

        Playlist playlist =
                findOwnedPlaylist(
                        userId,
                        playlistId
                );

        return PlaylistResponse.from(
                playlist
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistResponse>
            getAll(
                    UUID userId,
                    int page,
                    int size) {

        validatePagination(
                page,
                size
        );

        findUser(userId);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc(
                                        "updatedAt"
                                ),
                                Sort.Order.asc(
                                        "id"
                                )
                        )
                );

        Page<PlaylistResponse> playlists =
                playlistRepository
                        .findByUser_Id(
                                userId,
                                pageable
                        )
                        .map(
                                PlaylistResponse::from
                        );

        return PageResponse.from(
                playlists
        );
    }

    @Transactional
    public PlaylistResponse update(
            UUID userId,
            UUID playlistId,
            UpdatePlaylistRequest request) {

        Playlist playlist =
                findOwnedPlaylistForUpdate(
                        userId,
                        playlistId
                );

        playlist.update(
                request.name(),
                normalizeDescription(
                        request.description()
                )
        );

        return PlaylistResponse.from(
                playlist
        );
    }

    @Transactional
    public void delete(
            UUID userId,
            UUID playlistId) {

        Playlist playlist =
                findOwnedPlaylistForUpdate(
                        userId,
                        playlistId
                );

        playlistRepository.delete(
                playlist
        );
    }

    @Transactional
    public PlaylistTrackResponse addTrack(
            UUID userId,
            UUID playlistId,
            UUID trackId) {

        Playlist playlist =
                findOwnedPlaylistForUpdate(
                        userId,
                        playlistId
                );

        Track track =
                findTrack(trackId);

        PlaylistTrackId id =
                new PlaylistTrackId(
                        playlistId,
                        trackId
                );

        /*
         * PUT is idempotent.
         *
         * If this track is already in this playlist,
         * return the existing relationship.
         */
        PlaylistTrack existing =
                playlistTrackRepository
                        .findById(id)
                        .orElse(null);

        if (existing != null) {

            return PlaylistTrackResponse.from(
                    existing
            );
        }

        int nextPosition =
                playlistTrackRepository
                        .findMaxPosition(
                                playlistId
                        )
                        + 1;

        PlaylistTrack playlistTrack =
                new PlaylistTrack(
                        playlist,
                        track,
                        nextPosition
                );

        PlaylistTrack saved =
                playlistTrackRepository
                        .saveAndFlush(
                                playlistTrack
                        );

        playlist.touch();

        return PlaylistTrackResponse.from(
                saved
        );
    }

    @Transactional
    public void removeTrack(
            UUID userId,
            UUID playlistId,
            UUID trackId) {

        Playlist playlist =
                findOwnedPlaylistForUpdate(
                        userId,
                        playlistId
                );

        int deleted =
                playlistTrackRepository
                        .deleteTrack(
                                playlistId,
                                trackId
                        );

        if (deleted > 0) {
            playlist.touch();
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaylistTrackResponse>
            getTracks(
                    UUID userId,
                    UUID playlistId,
                    int page,
                    int size) {

        validatePagination(
                page,
                size
        );

        /*
         * Ownership is checked before reading tracks.
         *
         * Another user's playlist therefore behaves
         * like a nonexistent resource.
         */
        findOwnedPlaylist(
                userId,
                playlistId
        );

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<PlaylistTrackResponse> tracks =
                playlistTrackRepository
                        .findPageByPlaylistId(
                                playlistId,
                                pageable
                        )
                        .map(
                                PlaylistTrackResponse::from
                        );

        return PageResponse.from(
                tracks
        );
    }

    private UserAccount findUser(
            UUID userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: "
                                        + userId
                        )
                );
    }

    private Track findTrack(
            UUID trackId) {

        return trackRepository
                .findById(trackId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Track not found: "
                                        + trackId
                        )
                );
    }

    private Playlist findOwnedPlaylist(
            UUID userId,
            UUID playlistId) {

        return playlistRepository
                .findByIdAndUser_Id(
                        playlistId,
                        userId
                )
                .orElseThrow(() ->
                        playlistNotFound(
                                playlistId
                        )
                );
    }

    private Playlist findOwnedPlaylistForUpdate(
            UUID userId,
            UUID playlistId) {

        return playlistRepository
                .findOwnedPlaylistForUpdate(
                        playlistId,
                        userId
                )
                .orElseThrow(() ->
                        playlistNotFound(
                                playlistId
                        )
                );
    }

    private ResourceNotFoundException
            playlistNotFound(
                    UUID playlistId) {

        return new ResourceNotFoundException(
                "Playlist not found: "
                        + playlistId
        );
    }

    private String normalizeDescription(
            String description) {

        if (description == null
                || description.isBlank()) {

            return null;
        }

        return description.trim();
    }

    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {

            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1
                || size > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}
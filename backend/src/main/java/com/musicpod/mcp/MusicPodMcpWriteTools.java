package com.musicpod.mcp;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.musicpod.auth.CurrentUserProvider;
import com.musicpod.library.playlist.CreatePlaylistRequest;
import com.musicpod.library.playlist.PlaylistResponse;
import com.musicpod.library.playlist.PlaylistService;
import com.musicpod.library.playlist.PlaylistTrackResponse;

@Component
public class MusicPodMcpWriteTools {

    private static final int MAX_TRACKS_PER_REQUEST =
            50;

    private final CurrentUserProvider
            currentUserProvider;

    private final PlaylistService
            playlistService;

    public MusicPodMcpWriteTools(
            CurrentUserProvider currentUserProvider,
            PlaylistService playlistService) {

        this.currentUserProvider =
                currentUserProvider;

        this.playlistService =
                playlistService;
    }

    @McpTool(
            name = "musicpod_create_playlist",
            title = "Create Playlist",
            description =
                    "Creates a playlist owned by the "
                    + "currently authenticated MusicPod user.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = false,
                            destructiveHint = false,
                            idempotentHint = false,
                            openWorldHint = false
                    )
    )
    public PlaylistResponse createPlaylist(

            @McpToolParam(
                    description =
                            "Playlist name. Maximum 200 characters.",
                    required = true
            )
            String name,

            @McpToolParam(
                    description =
                            "Optional playlist description. "
                            + "Maximum 1000 characters.",
                    required = false
            )
            String description) {

        CreatePlaylistRequest request =
                validateCreateRequest(
                        name,
                        description
                );

        UUID userId =
                currentUserProvider.userId();

        return playlistService.create(
                userId,
                request
        );
    }

    @McpTool(
            name = "musicpod_add_tracks_to_playlist",
            title = "Add Tracks To Playlist",
            description =
                    "Adds one or more tracks to a playlist "
                    + "owned by the currently authenticated "
                    + "MusicPod user.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = false,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = false
                    )
    )
    public AddTracksToPlaylistResponse
            addTracksToPlaylist(

            @McpToolParam(
                    description =
                            "UUID of the playlist owned by "
                            + "the authenticated user.",
                    required = true
            )
            String playlistId,

            @McpToolParam(
                    description =
                            "Track UUIDs to add. "
                            + "Maximum 50 tracks.",
                    required = true
            )
            List<String> trackIds) {

        UUID parsedPlaylistId =
                parseUuid(
                        playlistId,
                        "Playlist ID"
                );

        List<UUID> parsedTrackIds =
                parseTrackIds(
                        trackIds
                );

        /*
         * Resolve identity only after validating
         * caller-controlled arguments.
         */
        UUID userId =
                currentUserProvider.userId();

        List<PlaylistTrackResponse> addedTracks =
                playlistService.addTracks(
                        userId,
                        parsedPlaylistId,
                        parsedTrackIds
                );

        return new AddTracksToPlaylistResponse(
                parsedPlaylistId,
                addedTracks
        );
    }

    private CreatePlaylistRequest
            validateCreateRequest(
                    String name,
                    String description) {

        CreatePlaylistRequest request =
                new CreatePlaylistRequest(
                        name,
                        description
                );

        if (request.name() == null
                || request.name().isBlank()) {

            throw new IllegalArgumentException(
                    "Playlist name is required"
            );
        }

        if (request.name().length() > 200) {

            throw new IllegalArgumentException(
                    "Playlist name must not exceed "
                            + "200 characters"
            );
        }

        if (request.description() != null
                && request.description().length()
                        > 1000) {

            throw new IllegalArgumentException(
                    "Playlist description must not exceed "
                            + "1000 characters"
            );
        }

        return request;
    }

    private List<UUID> parseTrackIds(
            List<String> trackIds) {

        if (trackIds == null
                || trackIds.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one track ID is required"
            );
        }

        if (trackIds.size()
                > MAX_TRACKS_PER_REQUEST) {

            throw new IllegalArgumentException(
                    "Cannot add more than "
                            + MAX_TRACKS_PER_REQUEST
                            + " tracks at once"
            );
        }

        LinkedHashSet<UUID> uniqueIds =
                new LinkedHashSet<>();

        for (String trackId : trackIds) {

            uniqueIds.add(
                    parseUuid(
                            trackId,
                            "Track ID"
                    )
            );
        }

        return new ArrayList<>(
                uniqueIds
        );
    }

    private UUID parseUuid(
            String value,
            String fieldName) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must not be blank"
            );
        }

        try {

            return UUID.fromString(
                    value.trim()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    fieldName
                            + " must be a valid UUID"
            );
        }
    }
}
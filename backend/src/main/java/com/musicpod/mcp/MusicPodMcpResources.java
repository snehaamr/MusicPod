package com.musicpod.mcp;

import java.util.Map;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;
import com.musicpod.library.playlist.PlaylistResponse;
import com.musicpod.library.playlist.PlaylistService;

import tools.jackson.databind.json.JsonMapper;

@Component
public class MusicPodMcpResources {

    private final TrackService
            trackService;

    private final PlaylistService
            playlistService;

    private final JsonMapper
            jsonMapper;

    private final McpAuditService
            mcpAuditService;

    public MusicPodMcpResources(
            TrackService trackService,
            PlaylistService playlistService,
            JsonMapper jsonMapper,
            McpAuditService mcpAuditService) {

        this.trackService =
                trackService;

        this.playlistService =
                playlistService;

        this.jsonMapper =
                jsonMapper;

        this.mcpAuditService =
                mcpAuditService;
    }

    @McpResource(
            uri = "musicpod://tracks/{trackId}",
            name = "musicpod_track",
            title = "MusicPod Track",
            description =
                    "Returns details for a MusicPod catalog track.",
            mimeType = "application/json"
    )
    public String getTrack(
            String trackId) {

        return mcpAuditService.executeJson(
                "musicpod_resource_track",
                false,
                McpAuditService.RISK_READ_ONLY,
                Map.of(
                        "trackId",
                        trackId == null
                                ? ""
                                : trackId
                ),
                userId -> {

                    UUID parsedTrackId =
                            parseUuid(
                                    trackId,
                                    "Track ID"
                            );

                    TrackResponse track =
                            trackService.getById(
                                    parsedTrackId
                            );

                    return jsonMapper
                            .writeValueAsString(
                                    track
                            );
                }
        );
    }

    @McpResource(
            uri = "musicpod://me/playlists/{playlistId}",
            name = "musicpod_owned_playlist",
            title = "My MusicPod Playlist",
            description =
                    "Returns playlist details only when the playlist "
                    + "belongs to the currently authenticated MusicPod user.",
            mimeType = "application/json"
    )
    public String getOwnedPlaylist(
            String playlistId) {

        return mcpAuditService.executeJson(
                "musicpod_resource_owned_playlist",
                false,
                McpAuditService.RISK_READ_ONLY,
                Map.of(
                        "playlistId",
                        playlistId == null
                                ? ""
                                : playlistId
                ),
                userId -> {

                    UUID parsedPlaylistId =
                            parseUuid(
                                    playlistId,
                                    "Playlist ID"
                            );

                    PlaylistResponse playlist =
                            playlistService.getById(
                                    userId,
                                    parsedPlaylistId
                            );

                    return jsonMapper
                            .writeValueAsString(
                                    playlist
                            );
                }
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
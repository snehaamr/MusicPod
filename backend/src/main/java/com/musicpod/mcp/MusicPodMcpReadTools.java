package com.musicpod.mcp;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;
import com.musicpod.search.track.HybridTrackSearchService;
import com.musicpod.search.track.TrackSearchResult;

@Component
public class MusicPodMcpReadTools {

    private static final int DEFAULT_SEARCH_SIZE =
            10;

    private final HybridTrackSearchService
            hybridTrackSearchService;

    private final TrackService
            trackService;

    private final McpAuditService
            mcpAuditService;

    public MusicPodMcpReadTools(
            HybridTrackSearchService hybridTrackSearchService,
            TrackService trackService,
            McpAuditService mcpAuditService) {

        this.hybridTrackSearchService =
                hybridTrackSearchService;

        this.trackService =
                trackService;

        this.mcpAuditService =
                mcpAuditService;
    }

    @McpTool(
            name = "musicpod_search_tracks",
            title = "Search MusicPod Tracks",
            description =
                    "Search the MusicPod catalog using hybrid "
                    + "lexical and semantic search. "
                    + "Use this to find tracks by title, artist, "
                    + "album, mood, theme, or natural-language description.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = false
                    )
    )
    public List<TrackSearchResult> searchTracks(

            @McpToolParam(
                    description =
                            "Natural-language track search query.",
                    required = true
            )
            String query,

            @McpToolParam(
                    description =
                            "Maximum number of results. "
                            + "Must be between 1 and 50. "
                            + "Defaults to 10.",
                    required = false
            )
            Integer size) {

        int requestedSize =
                size == null
                        ? DEFAULT_SEARCH_SIZE
                        : size;

        return mcpAuditService.execute(
                "musicpod_search_tracks",
                false,
                McpAuditService.RISK_READ_ONLY,
                Map.of(
                        "query",
                        query == null
                                ? ""
                                : query,
                        "size",
                        requestedSize
                ),
                userId ->
                        hybridTrackSearchService.search(
                                query,
                                requestedSize
                        )
        );
    }

    @McpTool(
            name = "musicpod_get_track",
            title = "Get MusicPod Track",
            description =
                    "Retrieve a MusicPod track by its track UUID.",
            generateOutputSchema = true,
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = false
                    )
    )
    public TrackResponse getTrack(

            @McpToolParam(
                    description =
                            "MusicPod track UUID.",
                    required = true
            )
            String trackId) {

        return mcpAuditService.execute(
                "musicpod_get_track",
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
                            parseTrackId(
                                    trackId
                            );

                    return trackService.getById(
                            parsedTrackId
                    );
                }
        );
    }

    private UUID parseTrackId(
            String trackId) {

        if (trackId == null
                || trackId.isBlank()) {

            throw new IllegalArgumentException(
                    "Track ID is required"
            );
        }

        try {

            return UUID.fromString(
                    trackId.trim()
            );

        } catch (IllegalArgumentException exception) {

            throw new IllegalArgumentException(
                    "Track ID must be a valid UUID",
                    exception
            );
        }
    }
}
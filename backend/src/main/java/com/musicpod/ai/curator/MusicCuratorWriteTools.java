package com.musicpod.ai.curator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.musicpod.auth.CurrentUserProvider;
import com.musicpod.library.playlist.CreatePlaylistRequest;
import com.musicpod.library.playlist.PlaylistResponse;
import com.musicpod.library.playlist.PlaylistService;
import com.musicpod.library.playlist.PlaylistTrackResponse;

@Component
public class MusicCuratorWriteTools {

    private static final int MAX_TRACKS_PER_REQUEST = 25;

    private final CurrentUserProvider currentUserProvider;
    private final PlaylistService playlistService;

    public MusicCuratorWriteTools(
            CurrentUserProvider currentUserProvider,
            PlaylistService playlistService) {

        this.currentUserProvider =
                currentUserProvider;

        this.playlistService =
                playlistService;
    }

    @Tool(description = """
            Create a new playlist for the currently authenticated
            MusicPod user.

            Only use this tool when the user explicitly asks to
            create or save a playlist.

            Do not call this tool merely because the user asks for
            recommendations or asks what a playlist could look like.

            The authenticated user is determined by MusicPod from
            the JWT.
            """)
    public PlaylistResponse createPlaylist(

            @ToolParam(
                    description =
                            "Name for the playlist"
            )
            String name,

            @ToolParam(
                    description =
                            "Optional short description for the playlist",
                    required = false
            )
            String description) {

        UUID userId =
                currentUserProvider.userId();

        CreatePlaylistRequest request =
                new CreatePlaylistRequest(
                        name,
                        description
                );

        return playlistService.create(
                userId,
                request
        );
    }

    @Tool(description = """
            Add MusicPod tracks to an existing playlist owned by the
            currently authenticated user.

            The playlist ID must come from a MusicPod tool result.

            Every track ID must come from MusicPod search or track
            tools. Never invent playlist IDs or track IDs.

            Existing MusicPod playlist behavior makes adding the same
            track again idempotent.
            """)
    public List<PlaylistTrackResponse> addTracksToPlaylist(

            @ToolParam(
                    description =
                            "MusicPod playlist UUID returned by createPlaylist or another MusicPod tool"
            )
            String playlistId,

            @ToolParam(
                    description =
                            "MusicPod track UUIDs to add to the playlist, in desired order"
            )
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
                            + " tracks in one request"
            );
        }

        UUID userId =
                currentUserProvider.userId();

        UUID parsedPlaylistId =
                UUID.fromString(
                        playlistId
                );

        List<PlaylistTrackResponse> results =
                new ArrayList<>();

        for (String trackId : trackIds) {

            UUID parsedTrackId =
                    UUID.fromString(
                            trackId
                    );

            PlaylistTrackResponse result =
                    playlistService.addTrack(
                            userId,
                            parsedPlaylistId,
                            parsedTrackId
                    );

            results.add(
                    result
            );
        }

        return results;
    }
}
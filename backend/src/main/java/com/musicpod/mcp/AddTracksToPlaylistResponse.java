package com.musicpod.mcp;

import java.util.List;
import java.util.UUID;

import com.musicpod.library.playlist.PlaylistTrackResponse;

public record AddTracksToPlaylistResponse(

        UUID playlistId,

        List<PlaylistTrackResponse> tracks

) {
}
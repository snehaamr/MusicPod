package com.musicpod.search;

public final class SearchIndexNames {

    /*
     * Stable alias used by normal reads and writes.
     */
    public static final String TRACKS =
            "musicpod-tracks";

    /*
     * Temporary physical target used while
     * building the next index version.
     */
    public static final String TRACKS_REINDEX_TARGET =
            "musicpod-tracks-v2";

    private SearchIndexNames() {
    }
}
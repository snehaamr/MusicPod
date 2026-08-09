package com.musicpod.messaging.kafka;

public final class KafkaTopics {

    public static final String PLAYBACK_RECORDED =
            "musicpod.playback.recorded.v1";

    public static final String TRACK_SEARCH_CHANGED =
            "musicpod.catalog.track.search.v1";

    private KafkaTopics() {
    }
}
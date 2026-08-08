package com.musicpod.messaging.outbox;

import org.springframework.stereotype.Service;

import com.musicpod.messaging.event.PlaybackRecordedEvent;
import com.musicpod.messaging.kafka.KafkaTopics;

import tools.jackson.databind.json.JsonMapper;

@Service
public class OutboxService {

    private static final String TRACK_AGGREGATE =
            "TRACK";

    private final OutboxEventRepository
            outboxEventRepository;

    private final JsonMapper jsonMapper;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper) {

        this.outboxEventRepository =
                outboxEventRepository;

        this.jsonMapper =
                jsonMapper;
    }

    public void enqueue(
            PlaybackRecordedEvent event) {

        String payload =
                jsonMapper.writeValueAsString(
                        event
                );

        outboxEventRepository.insert(
                event.eventId(),
                TRACK_AGGREGATE,
                event.trackId(),
                PlaybackRecordedEvent.EVENT_TYPE,
                KafkaTopics.PLAYBACK_RECORDED,
                event.trackId().toString(),
                payload
        );
    }
}
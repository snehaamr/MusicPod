package com.musicpod.messaging.outbox;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.musicpod.messaging.event.PlaybackRecordedEvent;
import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
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

        enqueue(
                event.eventId(),
                TRACK_AGGREGATE,
                event.trackId(),
                PlaybackRecordedEvent.EVENT_TYPE,
                KafkaTopics.PLAYBACK_RECORDED,
                event.trackId().toString(),
                event
        );
    }

    public void enqueue(
            TrackSearchUpsertedEvent event) {

        enqueue(
                event.eventId(),
                TRACK_AGGREGATE,
                event.trackId(),
                TrackSearchUpsertedEvent.EVENT_TYPE,
                KafkaTopics.TRACK_SEARCH_CHANGED,
                event.trackId().toString(),
                event
        );
    }

    public void enqueue(
            TrackSearchDeletedEvent event) {

        enqueue(
                event.eventId(),
                TRACK_AGGREGATE,
                event.trackId(),
                TrackSearchDeletedEvent.EVENT_TYPE,
                KafkaTopics.TRACK_SEARCH_CHANGED,
                event.trackId().toString(),
                event
        );
    }

    private void enqueue(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String messageKey,
            Object event) {

        String payload =
                jsonMapper.writeValueAsString(
                        event
                );

        outboxEventRepository.insert(
                eventId,
                aggregateType,
                aggregateId,
                eventType,
                topic,
                messageKey,
                payload
        );
    }
}
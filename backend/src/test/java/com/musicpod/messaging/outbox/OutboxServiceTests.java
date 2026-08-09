package com.musicpod.messaging.outbox;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.messaging.event.PlaybackRecordedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
import com.musicpod.messaging.kafka.KafkaTopics;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTests {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonMapper jsonMapper;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {

        outboxService =
                new OutboxService(
                        outboxEventRepository,
                        jsonMapper
                );
    }

    @Test
    void enqueuesPlaybackEvent() {

        PlaybackRecordedEvent event =
                org.mockito.Mockito.mock(
                        PlaybackRecordedEvent.class
                );

        UUID eventId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                event.trackId()
        ).thenReturn(
                trackId
        );

        when(
                jsonMapper.writeValueAsString(
                        event
                )
        ).thenReturn(
                "{\"event\":\"playback\"}"
        );

        outboxService.enqueue(
                event
        );

        verify(
                outboxEventRepository
        ).insert(
                eventId,
                "TRACK",
                trackId,
                PlaybackRecordedEvent.EVENT_TYPE,
                KafkaTopics.PLAYBACK_RECORDED,
                trackId.toString(),
                "{\"event\":\"playback\"}"
        );
    }

    @Test
    void enqueuesTrackSearchEvent() {

        TrackSearchUpsertedEvent event =
                org.mockito.Mockito.mock(
                        TrackSearchUpsertedEvent.class
                );

        UUID eventId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                event.trackId()
        ).thenReturn(
                trackId
        );

        when(
                jsonMapper.writeValueAsString(
                        event
                )
        ).thenReturn(
                "{\"event\":\"track\"}"
        );

        outboxService.enqueue(
                event
        );

        verify(
                outboxEventRepository
        ).insert(
                eventId,
                "TRACK",
                trackId,
                TrackSearchUpsertedEvent.EVENT_TYPE,
                KafkaTopics.TRACK_SEARCH_CHANGED,
                trackId.toString(),
                "{\"event\":\"track\"}"
        );
    }

    @Test
    void serializationFailureDoesNotInsertOutboxRow() {

        TrackSearchUpsertedEvent event =
                org.mockito.Mockito.mock(
                        TrackSearchUpsertedEvent.class
                );

        UUID eventId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        when(
                event.eventId()
        ).thenReturn(
                eventId
        );

        when(
                event.trackId()
        ).thenReturn(
                trackId
        );

        when(
                jsonMapper.writeValueAsString(
                        event
                )
        ).thenThrow(
                new IllegalStateException(
                        "serialization failed"
                )
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        outboxService.enqueue(
                                event
                        )
        );

        verifyNoInteractions(
                outboxEventRepository
        );
    }
}
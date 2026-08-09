package com.musicpod.search.track;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;

@ExtendWith(MockitoExtension.class)
class TrackSearchProjectionConsumerTests {

    @Mock
    private TrackSearchProjectionService projectionService;

    @Mock
    private ConsumerRecord<String, Object> consumerRecord;

    private TrackSearchProjectionConsumer consumer;

    @BeforeEach
    void setUp() {

        consumer =
                new TrackSearchProjectionConsumer(
                        projectionService
                );
    }

    @Test
    void dispatchesUpsertEvent() {

        TrackSearchUpsertedEvent event =
                org.mockito.Mockito.mock(
                        TrackSearchUpsertedEvent.class
                );

        when(
                consumerRecord.value()
        ).thenReturn(
                event
        );

        consumer.consume(
                consumerRecord
        );

        verify(
                projectionService
        ).upsert(
                event
        );
    }

    @Test
    void dispatchesDeleteEvent() {

        TrackSearchDeletedEvent event =
                org.mockito.Mockito.mock(
                        TrackSearchDeletedEvent.class
                );

        when(
                consumerRecord.value()
        ).thenReturn(
                event
        );

        consumer.consume(
                consumerRecord
        );

        verify(
                projectionService
        ).delete(
                event
        );
    }

    @Test
    void rejectsUnsupportedKafkaPayload() {

        when(
                consumerRecord.value()
        ).thenReturn(
                "not-an-event"
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        consumer.consume(
                                consumerRecord
                        )
        );

        verifyNoInteractions(
                projectionService
        );
    }
}
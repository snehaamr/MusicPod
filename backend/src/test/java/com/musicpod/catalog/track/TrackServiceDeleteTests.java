package com.musicpod.catalog.track;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;

import com.musicpod.catalog.album.AlbumRepository;
import com.musicpod.messaging.event.TrackSearchDeletedEvent;

@ExtendWith(MockitoExtension.class)
class TrackServiceDeleteTests {

    @Mock
    private TrackRepository trackRepository;

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private TrackService trackService;

    @BeforeEach
    void setUp() {

        trackService =
                new TrackService(
                        trackRepository,
                        albumRepository,
                        applicationEventPublisher
                );
    }

    @Test
    void deleteRemovesTrackExactlyOnce() {

        UUID trackId =
                UUID.randomUUID();

        Track track =
                org.mockito.Mockito.mock(
                        Track.class
                );

        when(
                trackRepository.findById(
                        trackId
                )
        ).thenReturn(
                Optional.of(
                        track
                )
        );

        when(
                track.getId()
        ).thenReturn(
                trackId
        );

        trackService.delete(
                trackId
        );

        verify(
                trackRepository,
                times(1)
        ).delete(
                track
        );

        verify(
                applicationEventPublisher
        ).publishEvent(
                any(
                        TrackSearchDeletedEvent.class
                )
        );
    }
}
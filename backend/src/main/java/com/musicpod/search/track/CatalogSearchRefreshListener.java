package com.musicpod.search.track;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.musicpod.catalog.track.Track;
import com.musicpod.messaging.event.AlbumUpdatedEvent;
import com.musicpod.messaging.event.ArtistUpdatedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
import com.musicpod.messaging.outbox.OutboxService;

@Component
public class CatalogSearchRefreshListener {

    private final TrackSearchSourceRepository
            trackSearchSourceRepository;

    private final OutboxService outboxService;

    public CatalogSearchRefreshListener(
            TrackSearchSourceRepository
                    trackSearchSourceRepository,
            OutboxService outboxService) {

        this.trackSearchSourceRepository =
                trackSearchSourceRepository;

        this.outboxService =
                outboxService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT
    )
    public void refreshAlbumTracks(
            AlbumUpdatedEvent event) {

        List<Track> tracks =
                trackSearchSourceRepository
                        .findAllForSearchByAlbumId(
                                event.albumId()
                        );

        enqueueTrackUpdates(tracks);
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT
    )
    public void refreshArtistTracks(
            ArtistUpdatedEvent event) {

        List<Track> tracks =
                trackSearchSourceRepository
                        .findAllForSearchByArtistId(
                                event.artistId()
                        );

        enqueueTrackUpdates(tracks);
    }

    private void enqueueTrackUpdates(
            List<Track> tracks) {

        for (Track track : tracks) {

            TrackSearchUpsertedEvent
                    searchEvent =
                    TrackSearchUpsertedEvent
                            .from(track);

            outboxService.enqueue(
                    searchEvent
            );
        }
    }
}
package com.musicpod.search.track;

import java.io.IOException;

import org.opensearch.client.opensearch.OpenSearchClient;

import org.springframework.stereotype.Service;

import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
import com.musicpod.search.SearchIndexNames;

@Service
public class TrackSearchProjectionService {

    private final OpenSearchClient
            openSearchClient;

    public TrackSearchProjectionService(
            OpenSearchClient openSearchClient) {

        this.openSearchClient =
                openSearchClient;
    }

    public void upsert(
            TrackSearchUpsertedEvent event) {

        TrackSearchDocument document =
                new TrackSearchDocument(
                        event.trackId(),
                        event.title(),
                        event.albumId(),
                        event.albumTitle(),
                        event.artistId(),
                        event.artistName(),
                        event.durationMs(),
                        event.explicit()
                );

        try {

            openSearchClient.index(
                    request ->
                            request
                                    .index(
                                            SearchIndexNames.TRACKS
                                    )
                                    .id(
                                            event.trackId()
                                                    .toString()
                                    )
                                    .document(
                                            document
                                    )
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to update track search projection",
                    exception
            );
        }
    }

    public void delete(
            TrackSearchDeletedEvent event) {

        try {

            openSearchClient.delete(
                    request ->
                            request
                                    .index(
                                            SearchIndexNames.TRACKS
                                    )
                                    .id(
                                            event.trackId()
                                                    .toString()
                                    )
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Failed to delete track search projection",
                    exception
            );
        }
    }
}
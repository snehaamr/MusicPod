package com.musicpod.messaging.event;

import java.util.UUID;

public record ArtistUpdatedEvent(
        UUID artistId
) {
}
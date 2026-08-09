package com.musicpod.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;

@Component
public class TrackSearchOutboxWriter {

    private final OutboxService outboxService;

    public TrackSearchOutboxWriter(
            OutboxService outboxService) {

        this.outboxService =
                outboxService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT
    )
    public void persistUpsert(
            TrackSearchUpsertedEvent event) {

        outboxService.enqueue(event);
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT
    )
    public void persistDelete(
            TrackSearchDeletedEvent event) {

        outboxService.enqueue(event);
    }
}
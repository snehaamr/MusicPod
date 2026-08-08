package com.musicpod.messaging.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

@Component
public class PlaybackOutboxWriter {

    private final OutboxService outboxService;

    public PlaybackOutboxWriter(
            OutboxService outboxService) {

        this.outboxService =
                outboxService;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.BEFORE_COMMIT
    )
    public void persist(
            PlaybackRecordedEvent event) {

        outboxService.enqueue(
                event
        );
    }
}
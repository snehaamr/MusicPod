package com.musicpod.messaging.outbox;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

@Component
public class OutboxMetrics
        implements MeterBinder {

    private final OutboxEventRepository
            outboxEventRepository;

    public OutboxMetrics(
            OutboxEventRepository
                    outboxEventRepository) {

        this.outboxEventRepository =
                outboxEventRepository;
    }

    @Override
    public void bindTo(
            MeterRegistry registry) {

        Gauge.builder(
                        "musicpod.outbox.pending",
                        outboxEventRepository,
                        repository ->
                                repository.countByStatus(
                                        "PENDING"
                                )
                )
                .description(
                        "Number of outbox events waiting to be published"
                )
                .register(registry);

        Gauge.builder(
                        "musicpod.outbox.processing",
                        outboxEventRepository,
                        repository ->
                                repository.countByStatus(
                                        "PROCESSING"
                                )
                )
                .description(
                        "Number of outbox events currently being processed"
                )
                .register(registry);

        Gauge.builder(
                        "musicpod.outbox.dead",
                        outboxEventRepository,
                        repository ->
                                repository.countByStatus(
                                        "DEAD"
                                )
                )
                .description(
                        "Number of outbox events that exhausted all publish attempts"
                )
                .register(registry);
    }
}
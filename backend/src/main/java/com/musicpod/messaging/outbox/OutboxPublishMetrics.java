package com.musicpod.messaging.outbox;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class OutboxPublishMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Timer publishDuration;

    public OutboxPublishMetrics(
            MeterRegistry meterRegistry) {

        this.meterRegistry =
                meterRegistry;

        this.publishSuccess =
                Counter.builder(
                                "musicpod.outbox.publish.success"
                        )
                        .description(
                                "Number of successfully published outbox events"
                        )
                        .register(
                                meterRegistry
                        );

        this.publishFailure =
                Counter.builder(
                                "musicpod.outbox.publish.failure"
                        )
                        .description(
                                "Number of failed outbox publish attempts"
                        )
                        .register(
                                meterRegistry
                        );

        this.publishDuration =
                Timer.builder(
                                "musicpod.outbox.publish.duration"
                        )
                        .description(
                                "Time spent attempting to publish an outbox event"
                        )
                        .register(
                                meterRegistry
                        );
    }

    public Timer.Sample startTimer() {

        return Timer.start(
                meterRegistry
        );
    }

    public void recordSuccess(
            Timer.Sample sample) {

        publishSuccess.increment();

        sample.stop(
                publishDuration
        );
    }

    public void recordFailure(
            Timer.Sample sample) {

        publishFailure.increment();

        sample.stop(
                publishDuration
        );
    }
}
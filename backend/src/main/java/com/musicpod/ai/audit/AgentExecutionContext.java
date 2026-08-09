package com.musicpod.ai.audit;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentExecutionContext {

    private final UUID runId;

    private final AtomicInteger sequence =
            new AtomicInteger(0);

    public AgentExecutionContext(
            UUID runId) {

        this.runId =
                runId;
    }

    public UUID runId() {
        return runId;
    }

    public int nextSequence() {
        return sequence.incrementAndGet();
    }
}
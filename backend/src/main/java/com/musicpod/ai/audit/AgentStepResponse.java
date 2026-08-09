package com.musicpod.ai.audit;

import java.time.Instant;
import java.util.UUID;

public record AgentStepResponse(

        UUID id,

        int sequenceNumber,

        String toolName,

        String toolRisk,

        AgentStepStatus status,

        Long durationMs,

        String input,

        String output,

        String error,

        Instant startedAt,

        Instant completedAt

) {

    public static AgentStepResponse from(
            AgentStep step) {

        return new AgentStepResponse(
                step.getId(),
                step.getSequenceNumber(),
                step.getToolName(),
                step.getToolRisk(),
                step.getStatus(),
                step.getDurationMs(),
                step.getInputJson(),
                step.getOutputJson(),
                step.getErrorMessage(),
                step.getStartedAt(),
                step.getCompletedAt()
        );
    }
}
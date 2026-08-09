package com.musicpod.ai.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentRunResponse(

        UUID id,

        String prompt,

        boolean allowWrite,

        AgentRunStatus status,

        String response,

        String error,

        Instant startedAt,

        Instant completedAt,

        List<AgentStepResponse> steps

) {
}
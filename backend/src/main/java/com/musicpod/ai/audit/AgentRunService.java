package com.musicpod.ai.audit;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.exception.ResourceNotFoundException;


@Service
public class AgentRunService {

    private final AgentRunRepository agentRunRepository;
    private final AuditPayloadSanitizer sanitizer;

    public AgentRunService(
            AgentRunRepository agentRunRepository,
            AuditPayloadSanitizer sanitizer) {

        this.agentRunRepository =
                agentRunRepository;

        this.sanitizer =
                sanitizer;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public UUID start(
            UUID userId,
            String prompt,
            boolean allowWrite) {

        AgentRun run =
                new AgentRun(
                        userId,
                        prompt,
                        allowWrite
                );

        AgentRun savedRun =
                agentRunRepository.saveAndFlush(
                        run
                );

        return savedRun.getId();
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void complete(
            UUID runId,
            String response) {

        AgentRun run =
                findRun(runId);

        run.complete(
                sanitizer.response(
                        response
                )
        );
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void fail(
            UUID runId,
            Throwable throwable) {

        AgentRun run =
                findRun(runId);

        run.fail(
                sanitizer.error(
                        errorMessage(
                                throwable
                        )
                )
        );
    }

    private AgentRun findRun(
            UUID runId) {

        return agentRunRepository
                .findById(runId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent run not found: "
                                        + runId
                        )
                );
    }

    private String errorMessage(
            Throwable throwable) {

        if (throwable == null) {
            return "Unknown agent execution error";
        }

        if (throwable.getMessage() != null
                && !throwable.getMessage().isBlank()) {

            return throwable.getMessage();
        }

        return throwable
                .getClass()
                .getSimpleName();
    }
}
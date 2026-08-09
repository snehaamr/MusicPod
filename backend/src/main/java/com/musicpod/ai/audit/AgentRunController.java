package com.musicpod.ai.audit;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.musicpod.auth.CurrentUserProvider;

@RestController
@RequestMapping("/api/v1/ai/runs")
public class AgentRunController {

    private final CurrentUserProvider
            currentUserProvider;

    private final AgentAuditQueryService
            queryService;

    public AgentRunController(
            CurrentUserProvider currentUserProvider,
            AgentAuditQueryService queryService) {

        this.currentUserProvider =
                currentUserProvider;

        this.queryService =
                queryService;
    }

    @GetMapping("/{runId}")
    public ResponseEntity<AgentRunResponse> getRun(
            @PathVariable UUID runId) {

        UUID userId =
                currentUserProvider.userId();

        return ResponseEntity.ok(
                queryService.getRun(
                        userId,
                        runId
                )
        );
    }
}
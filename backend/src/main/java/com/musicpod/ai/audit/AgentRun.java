package com.musicpod.ai.audit;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_runs")
public class AgentRun {

    @Id
    private UUID id;

    @Column(
            name = "user_id",
            nullable = false
    )
    private UUID userId;

    @Column(
            nullable = false,
            length = 1000
    )
    private String prompt;

    @Column(
            name = "allow_write",
            nullable = false
    )
    private boolean allowWrite;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 32
    )
    private AgentRunStatus status;

    @Column(
            columnDefinition = "text"
    )
    private String response;

    @Column(
            name = "error_message",
            columnDefinition = "text"
    )
    private String errorMessage;

    @Column(
            name = "started_at",
            nullable = false
    )
    private Instant startedAt;

    @Column(
            name = "completed_at"
    )
    private Instant completedAt;

    protected AgentRun() {
    }

    public AgentRun(
            UUID userId,
            String prompt,
            boolean allowWrite) {

        this.id =
                UUID.randomUUID();

        this.userId =
                userId;

        this.prompt =
                prompt;

        this.allowWrite =
                allowWrite;

        this.status =
                AgentRunStatus.RUNNING;

        this.startedAt =
                Instant.now();
    }

    public void complete(
            String response) {

        this.response =
                response;

        this.status =
                AgentRunStatus.COMPLETED;

        this.completedAt =
                Instant.now();
    }

    public void fail(
            String errorMessage) {

        this.errorMessage =
                errorMessage;

        this.status =
                AgentRunStatus.FAILED;

        this.completedAt =
                Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPrompt() {
        return prompt;
    }

    public boolean isAllowWrite() {
        return allowWrite;
    }

    public AgentRunStatus getStatus() {
        return status;
    }

    public String getResponse() {
        return response;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
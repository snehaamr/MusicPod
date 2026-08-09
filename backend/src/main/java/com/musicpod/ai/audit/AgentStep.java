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
@Table(name = "agent_steps")
public class AgentStep {

    @Id
    private UUID id;

    @Column(
            name = "run_id",
            nullable = false
    )
    private UUID runId;

    @Column(
            name = "sequence_number",
            nullable = false
    )
    private int sequenceNumber;

    @Column(
            name = "tool_name",
            nullable = false,
            length = 150
    )
    private String toolName;

    @Column(
            name = "tool_risk",
            nullable = false,
            length = 32
    )
    private String toolRisk;

    @Column(
            name = "input_json",
            columnDefinition = "text"
    )
    private String inputJson;

    @Column(
            name = "output_json",
            columnDefinition = "text"
    )
    private String outputJson;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 32
    )
    private AgentStepStatus status;

    @Column(
            name = "duration_ms"
    )
    private Long durationMs;

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

    protected AgentStep() {
    }

    public AgentStep(
            UUID runId,
            int sequenceNumber,
            String toolName,
            String toolRisk,
            String inputJson) {

        this.id =
                UUID.randomUUID();

        this.runId =
                runId;

        this.sequenceNumber =
                sequenceNumber;

        this.toolName =
                toolName;

        this.toolRisk =
                toolRisk;

        this.inputJson =
                inputJson;

        this.status =
                AgentStepStatus.RUNNING;

        this.startedAt =
                Instant.now();
    }

    public void complete(
            String outputJson,
            long durationMs) {

        this.outputJson =
                outputJson;

        this.durationMs =
                durationMs;

        this.status =
                AgentStepStatus.COMPLETED;

        this.completedAt =
                Instant.now();
    }

    public void fail(
            String errorMessage,
            long durationMs) {

        this.errorMessage =
                errorMessage;

        this.durationMs =
                durationMs;

        this.status =
                AgentStepStatus.FAILED;

        this.completedAt =
                Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getRunId() {
        return runId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolRisk() {
        return toolRisk;
    }

    public String getInputJson() {
        return inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public AgentStepStatus getStatus() {
        return status;
    }

    public Long getDurationMs() {
        return durationMs;
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
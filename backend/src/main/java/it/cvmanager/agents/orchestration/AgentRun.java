package it.cvmanager.agents.orchestration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_run")
class AgentRun {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Workflow workflow;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private String input;

    @JdbcTypeCode(SqlTypes.JSON)
    private String output;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AgentRun() {
        // JPA
    }

    AgentRun(UUID userId, Workflow workflow, String inputJson) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.workflow = workflow;
        this.status = RunStatus.RUNNING;
        this.input = inputJson;
        this.createdAt = Instant.now();
    }

    void addTokens(int input, int output) {
        this.inputTokens += input;
        this.outputTokens += output;
    }

    void succeed(String outputJson) {
        this.status = RunStatus.SUCCEEDED;
        this.output = outputJson;
        this.finishedAt = Instant.now();
    }

    void fail(String error) {
        this.status = RunStatus.FAILED;
        this.error = error;
        this.finishedAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    Workflow getWorkflow() {
        return workflow;
    }

    RunStatus getStatus() {
        return status;
    }

    String getOutput() {
        return output;
    }

    String getError() {
        return error;
    }

    int getInputTokens() {
        return inputTokens;
    }

    int getOutputTokens() {
        return outputTokens;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getFinishedAt() {
        return finishedAt;
    }
}

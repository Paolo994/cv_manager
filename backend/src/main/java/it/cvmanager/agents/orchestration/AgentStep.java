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

/** Un passo di un run: una chiamata di un agente, con modello, token e output per debug e costi. */
@Entity
@Table(name = "agent_step")
class AgentStep {

    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(nullable = false, length = 50)
    private String agent;

    @Column(length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    private String output;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected AgentStep() {
        // JPA
    }

    AgentStep(UUID runId, String agent) {
        this.id = UUID.randomUUID();
        this.runId = runId;
        this.agent = agent;
        this.status = RunStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    void succeed(String model, String outputJson, int inputTokens, int outputTokens) {
        this.status = RunStatus.SUCCEEDED;
        this.model = model;
        this.output = outputJson;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
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

    String getAgent() {
        return agent;
    }

    String getModel() {
        return model;
    }

    RunStatus getStatus() {
        return status;
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

    Instant getStartedAt() {
        return startedAt;
    }

    Instant getFinishedAt() {
        return finishedAt;
    }
}

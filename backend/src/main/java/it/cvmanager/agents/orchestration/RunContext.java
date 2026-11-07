package it.cvmanager.agents.orchestration;

import it.cvmanager.shared.llm.LlmResult;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Contesto passato al corpo di un workflow: identifica run e utente e registra ogni chiamata
 * di agente come passo del run (stato, modello, token, output), notificando la SPA.
 */
public final class RunContext {

    private final UUID runId;
    private final UUID userId;
    private final AgentRunService service;

    RunContext(UUID runId, UUID userId, AgentRunService service) {
        this.runId = runId;
        this.userId = userId;
        this.service = service;
    }

    public UUID runId() {
        return runId;
    }

    public UUID userId() {
        return userId;
    }

    /** Esegue un agente come passo registrato del run e ne restituisce l'output. */
    public <T> T step(String agentName, Supplier<LlmResult<T>> agentCall) {
        return service.executeStep(this, agentName, agentCall);
    }
}

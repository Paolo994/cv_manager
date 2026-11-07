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
        return step(agentName, 1, agentCall);
    }

    /** Come {@link #step(String, Supplier)}, indicando il tentativo (2+ = revisione). */
    public <T> T step(String agentName, int attempt, Supplier<LlmResult<T>> agentCall) {
        return service.executeStep(this, agentName, attempt, agentCall);
    }

    /**
     * Esegue due rami in parallelo e attende entrambi. Un ramo null viene saltato (risultato null).
     * Se un ramo fallisce, l'eccezione si propaga e il run fallisce.
     */
    public <A, B> Both<A, B> parallel(Supplier<A> first, Supplier<B> second) {
        return service.parallel(first, second);
    }

    public record Both<A, B>(A first, B second) {
    }
}

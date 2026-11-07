package it.cvmanager.agents.orchestration;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Stato di un run come lo vede la SPA: stesso formato per GET /api/runs/{id} e per gli eventi SSE. */
public record RunView(
        UUID id,
        Workflow workflow,
        RunStatus status,
        String error,
        JsonNode output,
        int inputTokens,
        int outputTokens,
        Instant createdAt,
        Instant finishedAt,
        List<StepView> steps
) {

    public record StepView(
            String agent,
            String model,
            RunStatus status,
            String error,
            int inputTokens,
            int outputTokens,
            Instant startedAt,
            Instant finishedAt
    ) {
    }
}

package it.cvmanager.shared.llm;

import it.cvmanager.config.AppProperties.AgentSettings;

/** Una chiamata a Claude: istruzioni di sistema, messaggio utente e impostazioni del modello. */
public record LlmRequest(String model, int maxTokens, Double temperature, String system, String user) {

    public static LlmRequest of(AgentSettings settings, String system, String user) {
        return new LlmRequest(settings.model(), settings.maxTokens(), settings.temperature(), system, user);
    }
}

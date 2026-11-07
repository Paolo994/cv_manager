package it.cvmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

/**
 * Configurazione applicativa (prefisso "app").
 *
 * @param storage dove salvare i file caricati
 * @param agents  modello e limiti per ogni agente, per nome (es. "cv-parser")
 */
@ConfigurationProperties("app")
public record AppProperties(Storage storage, @DefaultValue Map<String, AgentSettings> agents) {

    public record Storage(@DefaultValue("./data/uploads") String localDir) {
    }

    /**
     * @param model       id del modello Claude, es. claude-sonnet-5-5
     * @param maxTokens   limite di token in output
     * @param temperature null = default del modello
     */
    public record AgentSettings(String model, @DefaultValue("4000") int maxTokens, Double temperature) {
    }

    public AgentSettings agent(String name) {
        AgentSettings settings = agents.get(name);
        if (settings == null || settings.model() == null || settings.model().isBlank()) {
            throw new IllegalStateException("Configurazione mancante: app.agents." + name + ".model");
        }
        return settings;
    }
}

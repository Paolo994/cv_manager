package it.cvmanager.shared.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;

/**
 * Claude via Spring AI con output strutturato nativo: lo schema JSON del record Java
 * viene inviato come output schema, quindi la risposta è JSON valido per costruzione.
 */
@Component
class SpringAiLlmGateway implements LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmGateway.class);

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    SpringAiLlmGateway(ChatClient.Builder chatClientBuilder, JsonMapper jsonMapper) {
        this.chatClient = chatClientBuilder.build();
        this.jsonMapper = jsonMapper;
    }

    @Override
    public <T> LlmResult<T> structured(LlmRequest request, Class<T> type) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(type, jsonMapper);
        AnthropicChatOptions.Builder options = AnthropicChatOptions.builder()
                .model(request.model())
                .maxTokens(request.maxTokens())
                .outputSchema(converter.getJsonSchema());
        if (request.temperature() != null) {
            options.temperature(request.temperature());
        }

        ChatResponse response;
        try {
            response = chatClient.prompt()
                    .options(options)
                    .system(request.system())
                    .user(request.user())
                    .call()
                    .chatResponse();
        } catch (RuntimeException e) {
            log.warn("Chiamata a {} fallita: {}", request.model(), e.toString());
            throw new LlmException(userMessage(e), e);
        }

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new LlmException("Il modello non ha restituito una risposta.", null);
        }
        String text = response.getResult().getOutput().getText();
        String finishReason = response.getResult().getMetadata().getFinishReason();

        T value;
        try {
            value = converter.convert(text);
        } catch (RuntimeException e) {
            log.warn("Risposta di {} non conforme allo schema {} (finishReason={})", request.model(), type.getSimpleName(), finishReason);
            String message = finishReason != null && finishReason.toLowerCase(Locale.ROOT).contains("max")
                    ? "La risposta del modello è stata troncata: aumenta max-tokens per questo agente."
                    : "La risposta del modello non è nel formato atteso.";
            throw new LlmException(message, e);
        }

        Usage usage = response.getMetadata().getUsage();
        int inputTokens = usage != null && usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
        int outputTokens = usage != null && usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
        log.info("LLM {} -> {}: {} token in, {} token out", request.model(), type.getSimpleName(), inputTokens, outputTokens);
        return new LlmResult<>(value, request.model(), inputTokens, outputTokens);
    }

    private static String userMessage(RuntimeException e) {
        String text = String.valueOf(e.getMessage()).toLowerCase(Locale.ROOT);
        if (text.contains("401") || text.contains("authentication") || text.contains("api key") || text.contains("api-key")) {
            return "Chiave API di Anthropic mancante o non valida.";
        }
        if (text.contains("429") || text.contains("rate limit")) {
            return "Limite di richieste verso Claude raggiunto. Riprova tra poco.";
        }
        if (text.contains("model") && (text.contains("not found") || text.contains("404"))) {
            return "Modello Claude non disponibile: controlla la configurazione app.agents.";
        }
        return "Il servizio AI non ha risposto correttamente. Riprova tra poco.";
    }
}

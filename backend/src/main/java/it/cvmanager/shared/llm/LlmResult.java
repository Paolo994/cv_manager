package it.cvmanager.shared.llm;

/** Risultato tipizzato di una chiamata, con il consumo di token per il tracciamento dei costi. */
public record LlmResult<T>(T value, String model, int inputTokens, int outputTokens) {
}

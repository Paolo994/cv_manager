package it.cvmanager.shared.llm;

/**
 * Unico punto di accesso al modello. Gli agenti dipendono da questa interfaccia,
 * così nei test si sostituisce senza chiamare l'API.
 */
public interface LlmGateway {

    /** Chiede al modello una risposta conforme allo schema JSON del tipo indicato. */
    <T> LlmResult<T> structured(LlmRequest request, Class<T> type);
}

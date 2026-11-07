package it.cvmanager.shared.llm;

/** Errore di una chiamata al modello, con un messaggio mostrabile all'utente. */
public class LlmException extends RuntimeException {

    public LlmException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}

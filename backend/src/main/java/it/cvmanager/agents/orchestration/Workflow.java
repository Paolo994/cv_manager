package it.cvmanager.agents.orchestration;

/** Sequenze di agenti eseguite dall'orchestratore. */
public enum Workflow {
    /** CV Parser: testo del CV -> profilo strutturato (bozza). */
    IMPORT_CV,
    /** CV Reviewer: CV (+ annuncio opzionale) -> valutazione. */
    REVIEW_CV
}

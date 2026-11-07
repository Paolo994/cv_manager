package it.cvmanager.agents.orchestration;

/** Sequenze di agenti eseguite dall'orchestratore. */
public enum Workflow {
    /** CV Parser: testo del CV -> profilo strutturato (bozza). */
    IMPORT_CV,
    /** CV Reviewer: CV (+ annuncio opzionale) -> valutazione. */
    REVIEW_CV,
    /** Job Analyzer: testo dell'annuncio -> requisiti strutturati. */
    ANALYZE_JOB,
    /** Job Analyzer (se serve) -> CV Tailor + Cover Letter Writer in parallelo -> Critic, con al massimo 2 revisioni. */
    PREPARE_APPLICATION
}

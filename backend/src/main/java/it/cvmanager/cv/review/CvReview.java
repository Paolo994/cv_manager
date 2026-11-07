package it.cvmanager.cv.review;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Valutazione di un CV prodotta dal CV Reviewer. */
public record CvReview(
        @JsonPropertyDescription("Valutazione complessiva da 0 a 100, coerente con i punteggi dei criteri")
        int overallScore,
        @JsonPropertyDescription("Giudizio complessivo in 2-3 frasi")
        String summary,
        @JsonPropertyDescription("Un elemento per criterio valutato")
        List<CriterionScore> criteria,
        @JsonPropertyDescription("Da 2 a 5 punti di forza specifici del CV")
        List<String> strengths,
        @JsonPropertyDescription("Da 3 a 10 interventi concreti, ordinati per priorità")
        List<Suggestion> suggestions
) {

    public enum Criterion {
        CLARITY, IMPACT, STRUCTURE, ATS_COMPATIBILITY, JOB_FIT
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public record CriterionScore(
            Criterion criterion,
            @JsonPropertyDescription("Punteggio da 0 a 100")
            int score,
            @JsonPropertyDescription("Motivazione in 1-2 frasi")
            String comment
    ) {
    }

    public record Suggestion(
            Priority priority,
            @JsonPropertyDescription("Sezione del CV interessata, es. Esperienze, Competenze, Profilo")
            String section,
            @JsonPropertyDescription("Il problema riscontrato")
            String issue,
            @JsonPropertyDescription("Cosa fare, in modo concreto")
            String recommendation,
            @JsonPropertyDescription("Esempio di riscrittura basato solo su fatti del CV, con segnaposto come [X%] per i dati mancanti; stringa vuota se non utile")
            String example
    ) {
    }

    /** Punteggi riportati nell'intervallo 0-100, nel caso il modello ne restituisca di fuori scala. */
    public CvReview normalized() {
        return new CvReview(clamp(overallScore), summary,
                criteria == null ? List.of() : criteria.stream()
                        .map(c -> new CriterionScore(c.criterion(), clamp(c.score()), c.comment())).toList(),
                strengths == null ? List.of() : strengths,
                suggestions == null ? List.of() : suggestions);
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}

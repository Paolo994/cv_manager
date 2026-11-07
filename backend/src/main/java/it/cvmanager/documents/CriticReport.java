package it.cvmanager.documents;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Esito del Critic: le affermazioni dei documenti confrontate con il profilo. */
public record CriticReport(
        @JsonPropertyDescription("Esito della verifica in 1-2 frasi in italiano")
        String summary,
        @JsonPropertyDescription("Problemi trovati; lista vuota se i documenti sono fedeli al profilo")
        List<Issue> issues
) {

    public enum IssueType {
        /** Fatto non presente né deducibile dal profilo: blocca. */
        UNSUPPORTED_CLAIM,
        /** Fatto che distorce il profilo (date, livelli, responsabilità): blocca. */
        INACCURATE,
        /** Problema di forma, non fattuale: solo segnalato. */
        STYLE;

        public boolean isBlocking() {
            return this != STYLE;
        }
    }

    public record Issue(
            DocumentType document,
            IssueType type,
            @JsonPropertyDescription("La frase esatta del documento")
            String excerpt,
            @JsonPropertyDescription("Il problema, in italiano")
            String problem,
            @JsonPropertyDescription("Come correggere restando fedeli al profilo, in italiano")
            String suggestion
    ) {
    }

    public List<Issue> issuesFor(DocumentType type) {
        return issues == null ? List.of() : issues.stream().filter(i -> i.document() == type).toList();
    }

    public boolean hasBlockingIssues(DocumentType type) {
        return issuesFor(type).stream().anyMatch(i -> i.type() != null && i.type().isBlocking());
    }
}

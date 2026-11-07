package it.cvmanager.jobs;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.cvmanager.shared.Language;

import java.util.List;

/** Dati strutturati di un annuncio, estratti dal Job Analyzer. */
public record JobAnalysis(
        @JsonPropertyDescription("Titolo del ruolo come scritto nell'annuncio")
        String title,
        @JsonPropertyDescription("Nome dell'azienda che assume; stringa vuota se non indicato")
        String company,
        @JsonPropertyDescription("Sede di lavoro; stringa vuota se non indicata")
        String location,
        WorkMode workMode,
        @JsonPropertyDescription("JUNIOR, MID, SENIOR, LEAD o EXECUTIVE se deducibile, altrimenti stringa vuota")
        String seniority,
        @JsonPropertyDescription("Lingua dell'annuncio")
        Language language,
        @JsonPropertyDescription("Requisiti obbligatori, brevi (es. '5+ anni con Java')")
        List<String> mustHave,
        @JsonPropertyDescription("Requisiti preferenziali o graditi")
        List<String> niceToHave,
        @JsonPropertyDescription("Principali responsabilità del ruolo, brevi")
        List<String> responsibilities,
        @JsonPropertyDescription("Da 5 a 15 parole chiave per ATS, come scritte nell'annuncio")
        List<String> keywords,
        @JsonPropertyDescription("Retribuzione o range se indicati, come testo; stringa vuota se assente")
        String salary,
        @JsonPropertyDescription("Sintesi del ruolo in 1-2 frasi in italiano")
        String summary
) {

    public enum WorkMode {
        REMOTE, HYBRID, ON_SITE, UNSPECIFIED
    }
}

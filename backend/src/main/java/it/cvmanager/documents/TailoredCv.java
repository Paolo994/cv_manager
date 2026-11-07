package it.cvmanager.documents;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** CV adattato a un annuncio dal CV Tailor, già nella forma in cui viene impaginato. */
public record TailoredCv(
        String fullName,
        @JsonPropertyDescription("Titolo professionale orientato al ruolo, coerente con l'esperienza reale")
        String headline,
        @JsonPropertyDescription("Profilo in 3-4 frasi che collega l'esperienza reale ai requisiti principali")
        String summary,
        @JsonPropertyDescription("Esperienze dalla più recente")
        List<Experience> experiences,
        List<Education> education,
        @JsonPropertyDescription("Da 2 a 5 gruppi di competenze, prima quelle richieste dall'annuncio")
        List<SkillGroup> skills,
        @JsonPropertyDescription("Lingue con livello, es. 'Inglese - B2'")
        List<String> languages,
        List<String> certifications,
        @JsonPropertyDescription("Da 3 a 6 frasi in italiano per l'utente: cosa è stato adattato e quali requisiti non sono coperti dal profilo")
        List<String> changes
) {

    public record Experience(
            String role,
            String company,
            String location,
            @JsonPropertyDescription("Periodo, es. '03/2022 - oggi' oppure '03/2022 - present'")
            String period,
            @JsonPropertyDescription("Da 2 a 5 punti che iniziano con un verbo, prima i risultati")
            List<String> bullets
    ) {
    }

    public record Education(
            String degree,
            String institution,
            String period,
            @JsonPropertyDescription("Indirizzo di studio e voto, se presenti")
            String details
    ) {
    }

    public record SkillGroup(String category, List<String> items) {
    }
}

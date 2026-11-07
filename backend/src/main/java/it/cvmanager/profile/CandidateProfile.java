package it.cvmanager.profile;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Profilo strutturato del candidato. È l'output del CV Parser e l'input di tutti gli agenti successivi.
 * Non contiene contatti (email, telefono, indirizzo): non servono agli agenti e restano fuori dai prompt.
 * Le descrizioni dei campi diventano parte dello schema JSON inviato a Claude.
 */
public record CandidateProfile(
        @JsonPropertyDescription("Nome e cognome")
        @Size(max = 120) String fullName,
        @JsonPropertyDescription("Una riga che descrive il profilo professionale, es. 'Sviluppatore backend Java con 6 anni di esperienza'")
        @Size(max = 200) String headline,
        @JsonPropertyDescription("Città e paese di residenza, se indicati")
        @Size(max = 120) String location,
        @JsonPropertyDescription("Sintesi del profilo in 2-4 frasi, basata solo sul contenuto del CV")
        @Size(max = 2000) String summary,
        @JsonPropertyDescription("Seniority stimata da anni di esperienza e responsabilità")
        @NotNull Seniority seniority,
        @JsonPropertyDescription("Anni di esperienza professionale complessiva, arrotondati per difetto; 0 se non determinabile")
        @Min(0) @Max(60) int yearsOfExperience,
        @JsonPropertyDescription("Esperienze lavorative, dalla più recente")
        @NotNull @Size(max = 40) List<@Valid Experience> experiences,
        @JsonPropertyDescription("Formazione, dalla più recente")
        @NotNull @Size(max = 20) List<@Valid Education> education,
        @JsonPropertyDescription("Competenze tecniche, strumenti, linguaggi e metodologie, senza duplicati")
        @NotNull @Size(max = 150) List<@Size(max = 80) String> technicalSkills,
        @JsonPropertyDescription("Soft skill, solo se esplicitamente citate nel CV")
        @NotNull @Size(max = 50) List<@Size(max = 80) String> softSkills,
        @JsonPropertyDescription("Lingue conosciute")
        @NotNull @Size(max = 20) List<@Valid LanguageSkill> languages,
        @JsonPropertyDescription("Certificazioni, con ente e anno se presenti")
        @NotNull @Size(max = 50) List<@Size(max = 200) String> certifications,
        @JsonPropertyDescription("Lingua in cui è scritto il CV: 'it' oppure 'en'")
        @Size(max = 5) String cvLanguage
) {

    public CandidateProfile withYearsOfExperience(int years) {
        return new CandidateProfile(fullName, headline, location, summary, seniority, years, experiences, education,
                technicalSkills, softSkills, languages, certifications, cvLanguage);
    }

    public record Experience(
            @Size(max = 150) String role,
            @Size(max = 150) String company,
            @Size(max = 120) String location,
            @JsonPropertyDescription("Data di inizio, formato YYYY-MM oppure YYYY")
            @Size(max = 10) String startDate,
            @JsonPropertyDescription("Data di fine, formato YYYY-MM oppure YYYY; stringa vuota se è il ruolo attuale")
            @Size(max = 10) String endDate,
            @JsonPropertyDescription("true se è il ruolo attuale")
            boolean current,
            @JsonPropertyDescription("Descrizione delle responsabilità, come nel CV")
            @Size(max = 4000) String description,
            @JsonPropertyDescription("Risultati concreti, preferibilmente misurabili, come scritti nel CV")
            @NotNull @Size(max = 30) List<@Size(max = 500) String> achievements,
            @JsonPropertyDescription("Tecnologie e strumenti usati in questa esperienza")
            @NotNull @Size(max = 60) List<@Size(max = 80) String> technologies
    ) {
    }

    public record Education(
            @Size(max = 200) String degree,
            @Size(max = 200) String field,
            @Size(max = 200) String institution,
            @Size(max = 10) String startDate,
            @Size(max = 10) String endDate,
            @JsonPropertyDescription("Voto finale se indicato, es. 110/110")
            @Size(max = 50) String grade
    ) {
    }

    public record LanguageSkill(
            @Size(max = 60) String language,
            @JsonPropertyDescription("Livello: madrelingua oppure livello QCER (A1-C2) o descrizione come nel CV")
            @Size(max = 60) String level
    ) {
    }
}

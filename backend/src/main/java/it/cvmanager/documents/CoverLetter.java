package it.cvmanager.documents;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Lettera di presentazione. La firma (nome) e l'intestazione con i contatti si aggiungono in fase di esportazione. */
public record CoverLetter(
        @JsonPropertyDescription("Oggetto della lettera, es. 'Candidatura per la posizione di Senior Backend Developer'")
        @Size(max = 300) String subject,
        @JsonPropertyDescription("Saluto iniziale, es. 'Gentile team di selezione di Acme,'")
        @NotBlank @Size(max = 200) String salutation,
        @JsonPropertyDescription("Da 3 a 4 paragrafi del corpo della lettera")
        @NotNull @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 3000) String> paragraphs,
        @JsonPropertyDescription("Formula di chiusura senza firma, es. 'Cordiali saluti'")
        @NotBlank @Size(max = 100) String closing
) {
}

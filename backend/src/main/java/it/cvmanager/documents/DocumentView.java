package it.cvmanager.documents;

import it.cvmanager.shared.Language;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Documento come lo vede la SPA. content è una CoverLetter o un TailoredCv a seconda di type;
 * reviewNotes sono le segnalazioni del Critic rimaste aperte su questa versione.
 */
public record DocumentView(
        UUID id,
        UUID jobId,
        DocumentType type,
        int version,
        String origin,
        Language language,
        UUID runId,
        JsonNode content,
        List<CriticReport.Issue> reviewNotes,
        Instant createdAt
) {
}

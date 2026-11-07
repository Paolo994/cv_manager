package it.cvmanager.jobs;

import java.time.Instant;
import java.util.UUID;

/** Annuncio come lo vedono la SPA e gli agenti. analysis è null finché il Job Analyzer non ha lavorato. */
public record JobPostingView(UUID id, String source, String url, String description, JobAnalysis analysis, Instant createdAt) {
}

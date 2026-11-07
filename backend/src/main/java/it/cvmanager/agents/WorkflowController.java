package it.cvmanager.agents;

import it.cvmanager.auth.AppUserPrincipal;
import it.cvmanager.documents.DocumentType;
import it.cvmanager.shared.Language;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

/** Avvio dei workflow: rispondono 202 con l'id del run, da seguire su /api/runs/{id}/events. */
@RestController
@RequestMapping("/api/workflows")
class WorkflowController {

    private final WorkflowService workflows;

    WorkflowController(WorkflowService workflows) {
        this.workflows = workflows;
    }

    @PostMapping("/import-cv")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RunStarted importCv(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody ImportCvRequest request) {
        return new RunStarted(workflows.importCv(user.id(), request.cvId()));
    }

    @PostMapping("/review-cv")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RunStarted reviewCv(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody ReviewCvRequest request) {
        return new RunStarted(workflows.reviewCv(user.id(), request.cvId(), request.jobDescription()));
    }

    @PostMapping("/analyze-job")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RunStarted analyzeJob(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody AnalyzeJobRequest request) {
        return new RunStarted(workflows.analyzeJob(user.id(), request.jobId()));
    }

    @PostMapping("/prepare-application")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RunStarted prepareApplication(@AuthenticationPrincipal AppUserPrincipal user,
                                  @Valid @RequestBody PrepareApplicationRequest request) {
        return new RunStarted(workflows.prepareApplication(user.id(), request.jobId(), request.documents(),
                request.language(), request.tone(), request.notes()));
    }

    record ImportCvRequest(@NotNull UUID cvId) {
    }

    record AnalyzeJobRequest(@NotNull UUID jobId) {
    }

    /**
     * @param language null = lingua dell'annuncio
     * @param notes    cosa il candidato vuole sottolineare nella lettera
     */
    record PrepareApplicationRequest(
            @NotNull UUID jobId,
            @NotEmpty Set<DocumentType> documents,
            Language language,
            CoverLetterWriterAgent.Tone tone,
            @Size(max = 1000) String notes) {
    }

    record ReviewCvRequest(@NotNull UUID cvId, @Size(max = 20_000) String jobDescription) {
    }

    record RunStarted(UUID runId) {
    }
}

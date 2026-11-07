package it.cvmanager.agents;

import it.cvmanager.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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

    record ImportCvRequest(@NotNull UUID cvId) {
    }

    record ReviewCvRequest(@NotNull UUID cvId, @Size(max = 20_000) String jobDescription) {
    }

    record RunStarted(UUID runId) {
    }
}

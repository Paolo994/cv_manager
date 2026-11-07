package it.cvmanager.jobs;

import it.cvmanager.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
class JobPostingController {

    private final JobPostingService service;

    JobPostingController(JobPostingService service) {
        this.service = service;
    }

    /** Annuncio incollato dall'utente (anche da LinkedIn): il server non scarica pagine esterne. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    JobPostingView create(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody CreateJobRequest request) {
        return service.createManual(user.id(), request.description(), request.url());
    }

    @GetMapping
    List<JobPostingView> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return service.list(user.id());
    }

    @GetMapping("/{id}")
    JobPostingView get(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        return service.get(user.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        service.delete(user.id(), id);
    }

    record CreateJobRequest(
            @NotBlank @Size(min = 100, max = 20_000, message = "L'annuncio deve avere tra 100 e 20.000 caratteri") String description,
            @Size(max = 1000) String url) {
    }
}

package it.cvmanager.profile.preferences;

import it.cvmanager.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
class JobPreferencesController {

    private final JobPreferencesService service;

    JobPreferencesController(JobPreferencesService service) {
        this.service = service;
    }

    @GetMapping
    JobPreferences get(@AuthenticationPrincipal AppUserPrincipal user) {
        return service.get(user.id());
    }

    @PutMapping
    JobPreferences save(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody JobPreferences preferences) {
        return service.save(user.id(), preferences);
    }
}

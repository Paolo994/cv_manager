package it.cvmanager.profile;

import it.cvmanager.auth.AppUserPrincipal;
import it.cvmanager.profile.dto.ProfileUpdateRequest;
import it.cvmanager.profile.dto.ProfileView;
import it.cvmanager.shared.web.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
class ProfileController {

    private final ProfileService profileService;

    ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /** 404 finché l'utente non ha importato un CV. */
    @GetMapping
    ProfileView get(@AuthenticationPrincipal AppUserPrincipal user) {
        return profileService.find(user.id()).orElseThrow(() -> new NotFoundException("Profilo"));
    }

    @PutMapping
    ProfileView update(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.update(user.id(), request.profile(), request.confirm());
    }
}

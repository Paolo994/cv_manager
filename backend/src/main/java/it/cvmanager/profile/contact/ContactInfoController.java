package it.cvmanager.profile.contact;

import it.cvmanager.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile/contact")
class ContactInfoController {

    private final ContactInfoService service;

    ContactInfoController(ContactInfoService service) {
        this.service = service;
    }

    @GetMapping
    ContactInfo get(@AuthenticationPrincipal AppUserPrincipal user) {
        return service.get(user.id(), user.email());
    }

    @PutMapping
    ContactInfo save(@AuthenticationPrincipal AppUserPrincipal user, @Valid @RequestBody ContactInfo contact) {
        return service.save(user.id(), contact);
    }
}

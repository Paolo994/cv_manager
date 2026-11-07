package it.cvmanager.auth;

import it.cvmanager.auth.dto.RegisterRequest;
import it.cvmanager.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login e logout sono gestiti dai filtri di Spring Security
 * (POST /api/auth/login, POST /api/auth/logout): vedi SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegistrationService registrationService;

    AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /** Genera il token CSRF: la risposta imposta il cookie XSRF-TOKEN letto dalla SPA. */
    @GetMapping("/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf(CsrfToken token) {
        token.getToken();
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        AppUser user = registrationService.register(request);
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return new UserResponse(principal.id(), principal.email(), principal.fullName(), principal.role().name());
    }
}

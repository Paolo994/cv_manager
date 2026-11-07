package it.cvmanager.auth;

import it.cvmanager.auth.dto.RegisterRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    RegistrationService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.USER);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Due registrazioni concorrenti con la stessa email
            throw new EmailAlreadyUsedException();
        }
    }
}

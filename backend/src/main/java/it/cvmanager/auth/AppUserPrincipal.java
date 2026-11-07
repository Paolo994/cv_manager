package it.cvmanager.auth;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Utente autenticato salvato nella sessione (serializzato da Spring Session JDBC).
 * Espone l'id, che i moduli usano per filtrare i dati per proprietario.
 */
public final class AppUserPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String fullName;
    private final Role role;
    private String passwordHash;

    private AppUserPrincipal(UUID id, String email, String fullName, Role role, String passwordHash) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.passwordHash = passwordHash;
    }

    static AppUserPrincipal from(AppUser user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getPasswordHash());
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public Role role() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /** Dopo il login l'hash non resta in sessione. */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}

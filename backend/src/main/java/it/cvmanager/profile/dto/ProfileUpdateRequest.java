package it.cvmanager.profile.dto;

import it.cvmanager.profile.CandidateProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * @param confirm true = l'utente conferma il profilo; false = salva come bozza
 */
public record ProfileUpdateRequest(@NotNull @Valid CandidateProfile profile, boolean confirm) {
}

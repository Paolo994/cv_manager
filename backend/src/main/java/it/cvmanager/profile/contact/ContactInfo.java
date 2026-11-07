package it.cvmanager.profile.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Contatti del candidato. Servono solo all'intestazione dei documenti esportati:
 * non fanno parte del profilo e non vengono mai inviati al modello.
 */
public record ContactInfo(
        @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 300) String linkedin,
        @Size(max = 300) String website,
        @Size(max = 120) String city
) {

    public static ContactInfo empty(String email) {
        return new ContactInfo(email, "", "", "", "");
    }
}

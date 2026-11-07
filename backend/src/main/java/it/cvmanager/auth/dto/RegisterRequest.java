package it.cvmanager.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        // BCrypt considera al massimo 72 byte
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 120) String fullName
) {
}

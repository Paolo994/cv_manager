package it.cvmanager.profile.preferences;

import it.cvmanager.profile.Seniority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Preferenze di ricerca lavoro, usate dal Query Planner e dai filtri delle offerte (fase 4).
 *
 * @param minAnnualSalaryEur RAL minima in euro; null = nessun vincolo
 * @param seniority          seniority cercata; null = quella del profilo
 */
public record JobPreferences(
        @NotNull @Size(max = 10) List<@Size(max = 100) String> targetRoles,
        @NotNull @Size(max = 10) List<@Size(max = 100) String> locations,
        @NotNull @Size(max = 3) List<WorkMode> workModes,
        @Min(0) @Max(1_000_000) Integer minAnnualSalaryEur,
        Seniority seniority,
        @NotNull @Size(max = 20) List<@Size(max = 100) String> excludedIndustries,
        @NotNull @Size(max = 2) List<ApplicationLanguage> applicationLanguages,
        boolean willingToRelocate
) {

    public enum WorkMode {
        REMOTE, HYBRID, ON_SITE
    }

    public enum ApplicationLanguage {
        IT, EN
    }

    public static JobPreferences empty() {
        return new JobPreferences(List.of(), List.of(), List.of(), null, null, List.of(),
                List.of(ApplicationLanguage.IT), false);
    }
}

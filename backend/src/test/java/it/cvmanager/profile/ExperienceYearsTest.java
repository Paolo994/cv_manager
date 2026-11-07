package it.cvmanager.profile;

import it.cvmanager.profile.CandidateProfile.Experience;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExperienceYearsTest {

    private static final YearMonth NOW = YearMonth.of(2026, 11);

    @Test
    void sumsConsecutiveJobsUpToNow() {
        // caso reale del test con Claude: 09/2019 - 02/2022, poi 03/2022 - oggi = 7 anni e 2 mesi
        List<Experience> jobs = List.of(job("2022-03", "", true), job("2019-09", "2022-02", false));
        assertThat(ExperienceYears.compute(jobs, NOW)).contains(7);
    }

    @Test
    void doesNotCountOverlappingPeriodsTwice() {
        List<Experience> jobs = List.of(job("2020-01", "2024-01", false), job("2022-01", "2023-01", false));
        assertThat(ExperienceYears.compute(jobs, NOW)).contains(4);
    }

    @Test
    void addsSeparatePeriodsAndSkipsGaps() {
        List<Experience> jobs = List.of(job("2015-01", "2017-01", false), job("2020-01", "2023-07", false));
        assertThat(ExperienceYears.compute(jobs, NOW)).contains(5); // 24 + 42 mesi = 66
    }

    @Test
    void yearOnlyDatesUseWholeYears() {
        assertThat(ExperienceYears.compute(List.of(job("2017", "2021", false)), NOW)).contains(4); // gen 2017 - dic 2021
    }

    @Test
    void emptyWhenNoReadableDates() {
        assertThat(ExperienceYears.compute(List.of(job("", "", false), job("estate 2019", "2020", false)), NOW)).isEmpty();
        assertThat(ExperienceYears.compute(List.of(), NOW)).isEmpty();
    }

    @Test
    void ignoresInvertedOrFutureDates() {
        assertThat(ExperienceYears.compute(List.of(job("2024-01", "2020-01", false)), NOW)).isEmpty();
        assertThat(ExperienceYears.compute(List.of(job("2025-11", "2030-01", false)), NOW)).contains(1);
    }

    private static Experience job(String start, String end, boolean current) {
        return new Experience("Ruolo", "Azienda", "", start, end, current, "", List.of(), List.of());
    }
}

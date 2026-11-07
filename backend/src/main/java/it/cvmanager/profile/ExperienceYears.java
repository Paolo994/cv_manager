package it.cvmanager.profile;

import it.cvmanager.profile.CandidateProfile.Experience;

import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anni di esperienza calcolati dalle date delle esperienze, unendo i periodi sovrapposti.
 * Un calcolo deterministico è più affidabile della stima di un LLM.
 */
public final class ExperienceYears {

    private static final Pattern DATE = Pattern.compile("^(\\d{4})(?:-(\\d{1,2}))?$");

    private ExperienceYears() {
    }

    /** Anni completi di esperienza, oppure vuoto se nessuna esperienza ha date leggibili. */
    public static Optional<Integer> compute(List<Experience> experiences, YearMonth now) {
        List<YearMonth[]> periods = new ArrayList<>();
        for (Experience e : experiences) {
            Optional<YearMonth> start = parse(e.startDate(), false);
            Optional<YearMonth> end = e.current() || isBlank(e.endDate()) ? Optional.of(now) : parse(e.endDate(), true);
            if (start.isEmpty() || end.isEmpty() || end.get().isBefore(start.get())) {
                continue;
            }
            YearMonth cappedEnd = end.get().isAfter(now) ? now : end.get();
            periods.add(new YearMonth[]{start.get(), cappedEnd});
        }
        if (periods.isEmpty()) {
            return Optional.empty();
        }

        periods.sort(Comparator.comparing(p -> p[0]));
        long months = 0;
        YearMonth currentStart = periods.getFirst()[0];
        YearMonth currentEnd = periods.getFirst()[1];
        for (YearMonth[] p : periods.subList(1, periods.size())) {
            if (!p[0].isAfter(currentEnd.plusMonths(1))) {
                currentEnd = p[1].isAfter(currentEnd) ? p[1] : currentEnd;
            } else {
                months += ChronoUnit.MONTHS.between(currentStart, currentEnd);
                currentStart = p[0];
                currentEnd = p[1];
            }
        }
        months += ChronoUnit.MONTHS.between(currentStart, currentEnd);
        return Optional.of((int) (months / 12));
    }

    /** "YYYY-MM" oppure "YYYY": senza mese, gennaio per l'inizio e dicembre per la fine. */
    static Optional<YearMonth> parse(String value, boolean isEnd) {
        if (isBlank(value)) {
            return Optional.empty();
        }
        Matcher m = DATE.matcher(value.strip());
        if (!m.matches()) {
            return Optional.empty();
        }
        int year = Integer.parseInt(m.group(1));
        int month = m.group(2) != null ? Integer.parseInt(m.group(2)) : (isEnd ? 12 : 1);
        if (month < 1 || month > 12) {
            return Optional.empty();
        }
        return Optional.of(YearMonth.of(year, month));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package it.cvmanager.shared;

import java.util.Locale;

/** Lingue supportate per CV e lettere. */
public enum Language {
    IT("italiano", Locale.ITALIAN),
    EN("inglese", Locale.ENGLISH);

    private final String italianName;
    private final Locale locale;

    Language(String italianName, Locale locale) {
        this.italianName = italianName;
        this.locale = locale;
    }

    /** Nome della lingua da usare nei prompt, es. "Scrivi in inglese". */
    public String italianName() {
        return italianName;
    }

    public Locale locale() {
        return locale;
    }
}

package it.cvmanager.documents.export;

import it.cvmanager.documents.CoverLetter;
import it.cvmanager.documents.TailoredCv;
import it.cvmanager.profile.contact.ContactInfo;
import it.cvmanager.shared.Language;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Trasforma CV e lettere in blocchi impaginabili. */
final class DocumentLayouts {

    private DocumentLayouts() {
    }

    static List<Block> cv(TailoredCv cv, ContactInfo contact, Language language) {
        boolean it = language == Language.IT;
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Title(nz(cv.fullName())));
        if (notBlank(cv.headline())) {
            blocks.add(new Block.Subtitle(cv.headline()));
        }
        String contacts = contactLine(contact);
        if (!contacts.isEmpty()) {
            blocks.add(new Block.Note(contacts));
        }

        if (notBlank(cv.summary())) {
            blocks.add(new Block.Section(it ? "Profilo" : "Profile"));
            blocks.add(new Block.Paragraph(cv.summary(), false));
        }

        if (hasItems(cv.experiences())) {
            blocks.add(new Block.Section(it ? "Esperienza professionale" : "Professional experience"));
            for (TailoredCv.Experience e : cv.experiences()) {
                blocks.add(new Block.Entry(join(" - ", e.role(), e.company()), nz(e.period())));
                if (notBlank(e.location())) {
                    blocks.add(new Block.Note(e.location()));
                }
                list(e.bullets()).forEach(b -> blocks.add(new Block.Bullet(b)));
                blocks.add(new Block.Space(4));
            }
        }

        if (hasItems(cv.education())) {
            blocks.add(new Block.Section(it ? "Formazione" : "Education"));
            for (TailoredCv.Education e : cv.education()) {
                blocks.add(new Block.Entry(join(" - ", e.degree(), e.institution()), nz(e.period())));
                if (notBlank(e.details())) {
                    blocks.add(new Block.Note(e.details()));
                }
            }
        }

        if (hasItems(cv.skills())) {
            blocks.add(new Block.Section(it ? "Competenze" : "Skills"));
            for (TailoredCv.SkillGroup group : cv.skills()) {
                String items = String.join(", ", list(group.items()));
                blocks.add(new Block.Paragraph(notBlank(group.category()) ? group.category() + ": " + items : items, false));
            }
        }

        if (hasItems(cv.languages())) {
            blocks.add(new Block.Section(it ? "Lingue" : "Languages"));
            blocks.add(new Block.Paragraph(String.join(" · ", cv.languages()), false));
        }

        if (hasItems(cv.certifications())) {
            blocks.add(new Block.Section(it ? "Certificazioni" : "Certifications"));
            cv.certifications().forEach(c -> blocks.add(new Block.Bullet(c)));
        }
        return blocks;
    }

    static List<Block> letter(CoverLetter letter, String fullName, ContactInfo contact, String company,
                              Language language, LocalDate date) {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block.Title(nz(fullName)));
        String contacts = contactLine(contact);
        if (!contacts.isEmpty()) {
            blocks.add(new Block.Note(contacts));
        }
        blocks.add(new Block.Space(18));

        String formattedDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(language.locale()).format(date);
        String place = contact != null && notBlank(contact.city()) ? contact.city() + ", " : "";
        blocks.add(new Block.Paragraph(place + formattedDate, false));
        if (notBlank(company)) {
            blocks.add(new Block.Paragraph(company, false));
        }
        blocks.add(new Block.Space(12));

        if (notBlank(letter.subject())) {
            String label = language == Language.IT ? "Oggetto: " : "Re: ";
            blocks.add(new Block.Paragraph(label + letter.subject(), true));
            blocks.add(new Block.Space(8));
        }
        blocks.add(new Block.Paragraph(nz(letter.salutation()), false));
        blocks.add(new Block.Space(4));
        for (String paragraph : list(letter.paragraphs())) {
            blocks.add(new Block.Paragraph(paragraph, false));
            blocks.add(new Block.Space(6));
        }
        blocks.add(new Block.Space(6));
        blocks.add(new Block.Paragraph(nz(letter.closing()), false));
        blocks.add(new Block.Paragraph(nz(fullName), false));
        return blocks;
    }

    private static String contactLine(ContactInfo c) {
        if (c == null) {
            return "";
        }
        return Stream.of(c.city(), c.email(), c.phone(), c.linkedin(), c.website())
                .filter(DocumentLayouts::notBlank)
                .map(String::strip)
                .collect(Collectors.joining(" · "));
    }

    private static String join(String separator, String... parts) {
        return Stream.of(parts).filter(DocumentLayouts::notBlank).map(String::strip).collect(Collectors.joining(separator));
    }

    private static <T> List<T> list(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }

    private static boolean hasItems(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}

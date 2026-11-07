package it.cvmanager.documents.export;

import it.cvmanager.documents.CoverLetter;
import it.cvmanager.documents.DocumentService;
import it.cvmanager.documents.DocumentService.StoredDocument;
import it.cvmanager.documents.TailoredCv;
import it.cvmanager.jobs.JobAnalysis;
import it.cvmanager.jobs.JobPostingService;
import it.cvmanager.profile.ProfileService;
import it.cvmanager.profile.contact.ContactInfo;
import it.cvmanager.profile.contact.ContactInfoService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Esporta un documento generato in PDF o DOCX, aggiungendo contatti e firma salvati sul server. */
@Service
public class DocumentExporter {

    public enum Format {
        PDF("application/pdf", "pdf"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

        private final String mediaType;
        private final String extension;

        Format(String mediaType, String extension) {
            this.mediaType = mediaType;
            this.extension = extension;
        }

        public String mediaType() {
            return mediaType;
        }
    }

    public record ExportedFile(String filename, String mediaType, byte[] content) {
    }

    private final DocumentService documents;
    private final JobPostingService jobs;
    private final ProfileService profiles;
    private final ContactInfoService contacts;
    private final Clock clock = Clock.systemDefaultZone();

    DocumentExporter(DocumentService documents, JobPostingService jobs, ProfileService profiles, ContactInfoService contacts) {
        this.documents = documents;
        this.jobs = jobs;
        this.profiles = profiles;
        this.contacts = contacts;
    }

    public ExportedFile export(UUID userId, String accountEmail, UUID documentId, Format format) {
        StoredDocument document = documents.load(userId, documentId);
        ContactInfo contact = contacts.get(userId, accountEmail);
        JobAnalysis analysis = jobs.get(userId, document.jobId()).analysis();
        String company = analysis == null ? "" : analysis.company();
        String role = analysis == null ? "" : analysis.title();

        List<Block> blocks;
        String baseName;
        String fullName;
        if (document.content() instanceof TailoredCv cv) {
            fullName = cv.fullName();
            blocks = DocumentLayouts.cv(cv, contact, document.language());
            baseName = "CV";
        } else {
            CoverLetter letter = (CoverLetter) document.content();
            fullName = profiles.find(userId).map(p -> p.profile().fullName()).orElse("");
            blocks = DocumentLayouts.letter(letter, fullName, contact, company, document.language(), LocalDate.now(clock));
            baseName = "Lettera";
        }

        String title = String.join(" - ", List.of(baseName, fullName, company).stream().filter(s -> s != null && !s.isBlank()).toList());
        byte[] bytes = format == Format.PDF ? PdfRenderer.render(blocks, title) : DocxRenderer.render(blocks, title);
        String filename = slug(baseName + "-" + fullName + "-" + (company.isBlank() ? role : company)) + "." + format.extension;
        return new ExportedFile(filename, format.mediaType(), bytes);
    }

    /** Nome file sicuro: solo lettere, numeri e trattini. */
    static String slug(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "").toLowerCase(Locale.ROOT);
        return slug.isEmpty() ? "documento" : slug.substring(0, Math.min(slug.length(), 80));
    }
}

package it.cvmanager.documents;

import it.cvmanager.documents.GeneratedDocument.Origin;
import it.cvmanager.jobs.JobPostingService;
import it.cvmanager.shared.Language;
import it.cvmanager.shared.web.ApiException;
import it.cvmanager.shared.web.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final TypeReference<List<CriticReport.Issue>> ISSUES = new TypeReference<>() {
    };

    private final GeneratedDocumentRepository documents;
    private final JobPostingService jobs;
    private final JsonMapper jsonMapper;

    DocumentService(GeneratedDocumentRepository documents, JobPostingService jobs, JsonMapper jsonMapper) {
        this.documents = documents;
        this.jobs = jobs;
        this.jsonMapper = jsonMapper;
    }

    /** Nuova versione generata dagli agenti, con le segnalazioni del Critic rimaste aperte. */
    @Transactional
    public UUID saveGenerated(UUID userId, UUID jobId, DocumentType type, Language language, UUID runId,
                              Object content, List<CriticReport.Issue> openIssues) {
        return save(userId, jobId, type, Origin.AI, language, runId, content, openIssues).getId();
    }

    /** Modifica dell'utente: nuova versione che parte dal documento indicato (solo lettere). */
    @Transactional
    public DocumentView saveUserEdit(UUID userId, UUID documentId, CoverLetter letter) {
        GeneratedDocument base = find(userId, documentId);
        if (base.getType() != DocumentType.COVER_LETTER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Modifica non supportata",
                    "Per ora si possono modificare solo le lettere: per il CV rigenera i documenti.");
        }
        return toView(save(userId, base.getJobId(), base.getType(), Origin.USER, base.getLanguage(), null, letter, List.of()));
    }

    @Transactional(readOnly = true)
    public List<DocumentView> listForJob(UUID userId, UUID jobId) {
        jobs.get(userId, jobId); // 404 se l'annuncio non è dell'utente
        return documents.findByUserIdAndJobIdOrderByTypeAscVersionDesc(userId, jobId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public StoredDocument load(UUID userId, UUID documentId) {
        GeneratedDocument document = find(userId, documentId);
        Object content = switch (document.getType()) {
            case COVER_LETTER -> jsonMapper.readValue(document.getContent(), CoverLetter.class);
            case TAILORED_CV -> jsonMapper.readValue(document.getContent(), TailoredCv.class);
        };
        return new StoredDocument(document.getId(), document.getJobId(), document.getType(), document.getVersion(),
                document.getLanguage(), content);
    }

    private GeneratedDocument save(UUID userId, UUID jobId, DocumentType type, Origin origin, Language language,
                                   UUID runId, Object content, List<CriticReport.Issue> openIssues) {
        int version = documents.maxVersion(userId, jobId, type) + 1;
        String notes = openIssues == null || openIssues.isEmpty() ? null : jsonMapper.writeValueAsString(openIssues);
        return documents.saveAndFlush(new GeneratedDocument(userId, jobId, type, version, origin, language, runId,
                jsonMapper.writeValueAsString(content), notes));
    }

    private GeneratedDocument find(UUID userId, UUID documentId) {
        return documents.findByIdAndUserId(documentId, userId).orElseThrow(() -> new NotFoundException("Documento"));
    }

    private DocumentView toView(GeneratedDocument d) {
        List<CriticReport.Issue> notes = d.getReviewNotes() == null ? List.of() : jsonMapper.readValue(d.getReviewNotes(), ISSUES);
        return new DocumentView(d.getId(), d.getJobId(), d.getType(), d.getVersion(), d.getOrigin().name(), d.getLanguage(),
                d.getRunId(), jsonMapper.readTree(d.getContent()), notes, d.getCreatedAt());
    }

    /** content è CoverLetter o TailoredCv a seconda di type. */
    public record StoredDocument(UUID id, UUID jobId, DocumentType type, int version, Language language, Object content) {
    }
}

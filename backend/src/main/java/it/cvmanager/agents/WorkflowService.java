package it.cvmanager.agents;

import it.cvmanager.agents.CoverLetterWriterAgent.Tone;
import it.cvmanager.agents.orchestration.AgentRunService;
import it.cvmanager.agents.orchestration.RunContext;
import it.cvmanager.agents.orchestration.Workflow;
import it.cvmanager.cv.CvService;
import it.cvmanager.cv.review.CvReview;
import it.cvmanager.cv.review.CvReviewService;
import it.cvmanager.documents.CoverLetter;
import it.cvmanager.documents.CriticReport;
import it.cvmanager.documents.DocumentService;
import it.cvmanager.documents.DocumentType;
import it.cvmanager.documents.TailoredCv;
import it.cvmanager.jobs.JobAnalysis;
import it.cvmanager.jobs.JobPostingService;
import it.cvmanager.jobs.JobPostingView;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.profile.ProfileService;
import it.cvmanager.profile.ProfileStatus;
import it.cvmanager.shared.Language;
import it.cvmanager.shared.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Definizione dei workflow: quali agenti, in che ordine, e dove finiscono i risultati.
 * I controlli (proprietà dei dati, profilo confermato) avvengono prima di avviare il run,
 * così l'utente riceve subito un errore chiaro invece di un run fallito.
 */
@Service
class WorkflowService {

    /** Il Critic può rimandare indietro i documenti al massimo due volte. */
    static final int MAX_REVISIONS = 2;

    private final AgentRunService runs;
    private final CvService cvService;
    private final CvReviewService reviewService;
    private final ProfileService profileService;
    private final JobPostingService jobService;
    private final DocumentService documentService;
    private final CvParserAgent parser;
    private final CvReviewerAgent reviewer;
    private final JobAnalyzerAgent analyzer;
    private final CvTailorAgent tailor;
    private final CoverLetterWriterAgent writer;
    private final CriticAgent critic;

    WorkflowService(AgentRunService runs, CvService cvService, CvReviewService reviewService, ProfileService profileService,
                    JobPostingService jobService, DocumentService documentService, CvParserAgent parser,
                    CvReviewerAgent reviewer, JobAnalyzerAgent analyzer, CvTailorAgent tailor,
                    CoverLetterWriterAgent writer, CriticAgent critic) {
        this.runs = runs;
        this.cvService = cvService;
        this.reviewService = reviewService;
        this.profileService = profileService;
        this.jobService = jobService;
        this.documentService = documentService;
        this.parser = parser;
        this.reviewer = reviewer;
        this.analyzer = analyzer;
        this.tailor = tailor;
        this.writer = writer;
        this.critic = critic;
    }

    /** CV Parser -> bozza del profilo (sostituisce quello attuale, da confermare). */
    UUID importCv(UUID userId, UUID cvId) {
        String cvText = cvService.extractedText(userId, cvId);
        return runs.start(userId, Workflow.IMPORT_CV, Map.of("cvId", cvId), ctx -> {
            CandidateProfile profile = ctx.step(CvParserAgent.NAME, () -> parser.parse(cvText));
            profileService.saveDraftFromParser(userId, cvId, profile);
            return Map.of("cvId", cvId, "profileStatus", "DRAFT");
        });
    }

    /** CV Reviewer -> valutazione salvata nello storico del CV. */
    UUID reviewCv(UUID userId, UUID cvId, String jobDescription) {
        String cvText = cvService.extractedText(userId, cvId);
        String job = jobDescription == null || jobDescription.isBlank() ? null : jobDescription.strip();
        return runs.start(userId, Workflow.REVIEW_CV, Map.of("cvId", cvId, "withJobDescription", job != null), ctx -> {
            CvReview review = ctx.step(CvReviewerAgent.NAME, () -> reviewer.review(cvText, job));
            UUID reviewId = reviewService.save(userId, cvId, ctx.runId(), job, review);
            return Map.of("cvId", cvId, "reviewId", reviewId, "overallScore", review.overallScore());
        });
    }

    /** Job Analyzer -> requisiti strutturati salvati sull'annuncio. */
    UUID analyzeJob(UUID userId, UUID jobId) {
        JobPostingView job = jobService.get(userId, jobId);
        return runs.start(userId, Workflow.ANALYZE_JOB, Map.of("jobId", jobId), ctx -> {
            analyze(ctx, userId, job);
            return Map.of("jobId", jobId);
        });
    }

    /**
     * Prepara i documenti per un annuncio:
     * Job Analyzer (se serve) -> CV Tailor e Cover Letter Writer in parallelo -> Critic.
     * Se il Critic trova affermazioni non supportate dal profilo, rimanda indietro solo il documento
     * interessato, al massimo {@value #MAX_REVISIONS} volte. Le segnalazioni rimaste restano visibili all'utente.
     */
    UUID prepareApplication(UUID userId, UUID jobId, Set<DocumentType> types, Language requestedLanguage, Tone tone, String notes) {
        if (types == null || types.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nessun documento", "Scegli almeno un documento da preparare.");
        }
        CandidateProfile profile = profileService.find(userId)
                .filter(p -> p.status() == ProfileStatus.CONFIRMED)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Profilo da confermare",
                        "Conferma il tuo profilo prima di preparare una candidatura: gli agenti usano solo dati confermati."))
                .profile();
        JobPostingView job = jobService.get(userId, jobId);
        boolean wantCv = types.contains(DocumentType.TAILORED_CV);
        boolean wantLetter = types.contains(DocumentType.COVER_LETTER);
        Tone chosenTone = tone == null ? Tone.PROFESSIONAL : tone;

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("jobId", jobId);
        input.put("documents", types);
        input.put("language", requestedLanguage);
        input.put("tone", chosenTone);

        return runs.start(userId, Workflow.PREPARE_APPLICATION, input, ctx -> {
            JobAnalysis analysis = job.analysis() != null ? job.analysis() : analyze(ctx, userId, job);
            Language language = requestedLanguage != null ? requestedLanguage
                    : analysis.language() != null ? analysis.language() : Language.IT;
            String description = job.description();

            // 1. Prima stesura, in parallelo
            RunContext.Both<TailoredCv, CoverLetter> drafts = ctx.parallel(
                    wantCv ? () -> ctx.step(CvTailorAgent.NAME, 1,
                            () -> tailor.tailor(profile, analysis, description, language, null, null)) : null,
                    wantLetter ? () -> ctx.step(CoverLetterWriterAgent.NAME, 1,
                            () -> writer.write(profile, analysis, description, language, chosenTone, notes, null, null)) : null);
            TailoredCv cv = drafts.first();
            CoverLetter letter = drafts.second();

            // 2. Verifica del Critic ed eventuali revisioni mirate
            CriticReport report = review(ctx, 1, profile, cv, letter);
            int revisions = 0;
            while (revisions < MAX_REVISIONS && hasBlocking(report)) {
                revisions++;
                int attempt = revisions + 1;
                CriticReport current = report;
                TailoredCv previousCv = cv;
                CoverLetter previousLetter = letter;
                RunContext.Both<TailoredCv, CoverLetter> revised = ctx.parallel(
                        current.hasBlockingIssues(DocumentType.TAILORED_CV) ? () -> ctx.step(CvTailorAgent.NAME, attempt,
                                () -> tailor.tailor(profile, analysis, description, language, previousCv,
                                        current.issuesFor(DocumentType.TAILORED_CV))) : null,
                        current.hasBlockingIssues(DocumentType.COVER_LETTER) ? () -> ctx.step(CoverLetterWriterAgent.NAME, attempt,
                                () -> writer.write(profile, analysis, description, language, chosenTone, notes, previousLetter,
                                        current.issuesFor(DocumentType.COVER_LETTER))) : null);
                if (revised.first() != null) {
                    cv = revised.first();
                }
                if (revised.second() != null) {
                    letter = revised.second();
                }
                report = review(ctx, attempt, profile, cv, letter);
            }

            // 3. Salvataggio, con le segnalazioni ancora aperte
            List<UUID> documentIds = new ArrayList<>();
            if (cv != null) {
                documentIds.add(documentService.saveGenerated(userId, jobId, DocumentType.TAILORED_CV, language, ctx.runId(),
                        cv, report.issuesFor(DocumentType.TAILORED_CV)));
            }
            if (letter != null) {
                documentIds.add(documentService.saveGenerated(userId, jobId, DocumentType.COVER_LETTER, language, ctx.runId(),
                        letter, report.issuesFor(DocumentType.COVER_LETTER)));
            }
            return Map.of("jobId", jobId, "documentIds", documentIds, "revisions", revisions,
                    "openIssues", report.issues().size(), "blockingIssues", hasBlocking(report));
        });
    }

    private JobAnalysis analyze(RunContext ctx, UUID userId, JobPostingView job) {
        JobAnalysis analysis = ctx.step(JobAnalyzerAgent.NAME, () -> analyzer.analyze(job.description()));
        jobService.saveAnalysis(userId, job.id(), analysis);
        return analysis;
    }

    private CriticReport review(RunContext ctx, int attempt, CandidateProfile profile, TailoredCv cv, CoverLetter letter) {
        return ctx.step(CriticAgent.NAME, attempt, () -> critic.review(profile, cv, letter));
    }

    private static boolean hasBlocking(CriticReport report) {
        return report.hasBlockingIssues(DocumentType.TAILORED_CV) || report.hasBlockingIssues(DocumentType.COVER_LETTER);
    }
}

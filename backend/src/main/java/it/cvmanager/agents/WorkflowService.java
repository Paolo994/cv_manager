package it.cvmanager.agents;

import it.cvmanager.agents.orchestration.AgentRunService;
import it.cvmanager.agents.orchestration.Workflow;
import it.cvmanager.cv.CvService;
import it.cvmanager.cv.review.CvReview;
import it.cvmanager.cv.review.CvReviewService;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.profile.ProfileService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Definizione dei workflow: quali agenti, in che ordine, e dove finiscono i risultati.
 * I controlli di proprietà avvengono prima di avviare il run, così un id altrui dà subito 404.
 */
@Service
class WorkflowService {

    private final AgentRunService runs;
    private final CvService cvService;
    private final CvReviewService reviewService;
    private final ProfileService profileService;
    private final CvParserAgent parser;
    private final CvReviewerAgent reviewer;

    WorkflowService(AgentRunService runs, CvService cvService, CvReviewService reviewService,
                    ProfileService profileService, CvParserAgent parser, CvReviewerAgent reviewer) {
        this.runs = runs;
        this.cvService = cvService;
        this.reviewService = reviewService;
        this.profileService = profileService;
        this.parser = parser;
        this.reviewer = reviewer;
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
}

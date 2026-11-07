package it.cvmanager.agents;

import it.cvmanager.config.AppProperties;
import it.cvmanager.cv.review.CvReview;
import it.cvmanager.shared.llm.LlmGateway;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import org.springframework.stereotype.Component;

/** Agente: valuta un CV, opzionalmente rispetto a un annuncio. */
@Component
public class CvReviewerAgent {

    public static final String NAME = "cv-reviewer";
    /** Lingua dei commenti: quella dell'interfaccia. */
    private static final String FEEDBACK_LANGUAGE = "italiano";

    private final LlmGateway llm;
    private final AppProperties properties;
    private final String systemPrompt = Prompts.load(NAME).replace("{language}", FEEDBACK_LANGUAGE);

    CvReviewerAgent(LlmGateway llm, AppProperties properties) {
        this.llm = llm;
        this.properties = properties;
    }

    public LlmResult<CvReview> review(String cvText, String jobDescription) {
        StringBuilder user = new StringBuilder("Valuta questo CV.\n\n").append(Prompts.document("cv_text", cvText));
        if (jobDescription != null && !jobDescription.isBlank()) {
            user.append("\n\nValutalo anche rispetto a questo annuncio, includendo il criterio JOB_FIT.\n\n")
                    .append(Prompts.document("job_description", jobDescription.strip()));
        }
        LlmResult<CvReview> result = llm.structured(
                LlmRequest.of(properties.agent(NAME), systemPrompt, user.toString()), CvReview.class);
        return new LlmResult<>(result.value().normalized(), result.model(), result.inputTokens(), result.outputTokens());
    }
}

package it.cvmanager.agents;

import it.cvmanager.config.AppProperties;
import it.cvmanager.documents.CriticReport;
import it.cvmanager.documents.TailoredCv;
import it.cvmanager.jobs.JobAnalysis;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.shared.Language;
import it.cvmanager.shared.llm.LlmGateway;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Profilo + annuncio -> CV adattato. In revisione riceve la bozza precedente e i problemi segnalati dal Critic. */
@Component
public class CvTailorAgent {

    public static final String NAME = "cv-tailor";

    private final LlmGateway llm;
    private final AppProperties properties;
    private final JsonMapper jsonMapper;
    private final String systemPrompt = Prompts.load(NAME);

    CvTailorAgent(LlmGateway llm, AppProperties properties, JsonMapper jsonMapper) {
        this.llm = llm;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public LlmResult<TailoredCv> tailor(CandidateProfile profile, JobAnalysis job, String jobDescription, Language language,
                                       TailoredCv previousDraft, List<CriticReport.Issue> feedback) {
        String system = systemPrompt.replace("{language}", language.italianName());
        StringBuilder user = new StringBuilder("Adatta il CV del candidato a questo annuncio.\n\n")
                .append(Prompts.document("candidate_profile", jsonMapper.writeValueAsString(profile))).append("\n\n")
                .append(Prompts.document("job_analysis", jsonMapper.writeValueAsString(job))).append("\n\n")
                .append(Prompts.document("job_description", jobDescription));
        Revisions.append(user, jsonMapper, previousDraft, feedback);
        return llm.structured(LlmRequest.of(properties.agent(NAME), system, user.toString()), TailoredCv.class);
    }
}

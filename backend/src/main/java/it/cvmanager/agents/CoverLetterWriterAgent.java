package it.cvmanager.agents;

import it.cvmanager.config.AppProperties;
import it.cvmanager.documents.CoverLetter;
import it.cvmanager.documents.CriticReport;
import it.cvmanager.jobs.JobAnalysis;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.shared.Language;
import it.cvmanager.shared.llm.LlmGateway;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Profilo + annuncio -> lettera di presentazione. In revisione riceve la bozza precedente e i problemi del Critic. */
@Component
public class CoverLetterWriterAgent {

    public static final String NAME = "cover-letter-writer";

    public enum Tone {
        FORMAL("formale"),
        PROFESSIONAL("professionale e cordiale"),
        DIRECT("diretto e sicuro");

        private final String description;

        Tone(String description) {
            this.description = description;
        }
    }

    private final LlmGateway llm;
    private final AppProperties properties;
    private final JsonMapper jsonMapper;
    private final String systemPrompt = Prompts.load(NAME);

    CoverLetterWriterAgent(LlmGateway llm, AppProperties properties, JsonMapper jsonMapper) {
        this.llm = llm;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public LlmResult<CoverLetter> write(CandidateProfile profile, JobAnalysis job, String jobDescription, Language language,
                                        Tone tone, String candidateNotes, CoverLetter previousDraft,
                                        List<CriticReport.Issue> feedback) {
        String system = systemPrompt.replace("{language}", language.italianName()).replace("{tone}", tone.description);
        StringBuilder user = new StringBuilder("Scrivi la lettera di presentazione per questo annuncio.\n\n")
                .append(Prompts.document("candidate_profile", jsonMapper.writeValueAsString(profile))).append("\n\n")
                .append(Prompts.document("job_analysis", jsonMapper.writeValueAsString(job))).append("\n\n")
                .append(Prompts.document("job_description", jobDescription));
        if (candidateNotes != null && !candidateNotes.isBlank()) {
            user.append("\n\n").append(Prompts.document("candidate_notes", candidateNotes.strip()));
        }
        Revisions.append(user, jsonMapper, previousDraft, feedback);
        return llm.structured(LlmRequest.of(properties.agent(NAME), system, user.toString()), CoverLetter.class);
    }
}

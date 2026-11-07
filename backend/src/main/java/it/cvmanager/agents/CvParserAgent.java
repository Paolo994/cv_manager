package it.cvmanager.agents;

import it.cvmanager.config.AppProperties;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.profile.ExperienceYears;
import it.cvmanager.shared.llm.LlmGateway;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;

/** Agente 1: testo del CV -> profilo strutturato. */
@Component
public class CvParserAgent {

    public static final String NAME = "cv-parser";

    private final LlmGateway llm;
    private final AppProperties properties;
    private final Clock clock;
    private final String systemPrompt = Prompts.load(NAME);

    @Autowired
    CvParserAgent(LlmGateway llm, AppProperties properties) {
        this(llm, properties, Clock.systemDefaultZone());
    }

    CvParserAgent(LlmGateway llm, AppProperties properties, Clock clock) {
        this.llm = llm;
        this.properties = properties;
        this.clock = clock;
    }

    public LlmResult<CandidateProfile> parse(String cvText) {
        String user = "Estrai il profilo da questo CV.\n\n" + Prompts.document("cv_text", cvText);
        LlmResult<CandidateProfile> result = llm.structured(
                LlmRequest.of(properties.agent(NAME), systemPrompt, user), CandidateProfile.class);
        // Gli anni si calcolano dalle date estratte: la stima del modello resta solo se le date non sono leggibili
        CandidateProfile profile = result.value();
        CandidateProfile withYears = ExperienceYears.compute(profile.experiences(), YearMonth.now(clock))
                .map(profile::withYearsOfExperience)
                .orElse(profile);
        return new LlmResult<>(withYears, result.model(), result.inputTokens(), result.outputTokens());
    }
}

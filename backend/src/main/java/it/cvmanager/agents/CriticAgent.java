package it.cvmanager.agents;

import it.cvmanager.config.AppProperties;
import it.cvmanager.documents.CoverLetter;
import it.cvmanager.documents.CriticReport;
import it.cvmanager.documents.TailoredCv;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.shared.llm.LlmGateway;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Confronta CV adattato e lettera con il profilo e segnala ogni affermazione non supportata. */
@Component
public class CriticAgent {

    public static final String NAME = "critic";

    private final LlmGateway llm;
    private final AppProperties properties;
    private final JsonMapper jsonMapper;
    private final String systemPrompt = Prompts.load(NAME);

    CriticAgent(LlmGateway llm, AppProperties properties, JsonMapper jsonMapper) {
        this.llm = llm;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    public LlmResult<CriticReport> review(CandidateProfile profile, TailoredCv cv, CoverLetter letter) {
        StringBuilder user = new StringBuilder("Verifica questi documenti rispetto al profilo.\n\n")
                .append(Prompts.document("candidate_profile", jsonMapper.writeValueAsString(profile)));
        if (cv != null) {
            user.append("\n\n").append(Prompts.document("tailored_cv", jsonMapper.writeValueAsString(cv)));
        }
        if (letter != null) {
            user.append("\n\n").append(Prompts.document("cover_letter", jsonMapper.writeValueAsString(letter)));
        }
        LlmResult<CriticReport> result = llm.structured(
                LlmRequest.of(properties.agent(NAME), systemPrompt, user.toString()), CriticReport.class);

        // Solo problemi sui documenti effettivamente verificati
        CriticReport report = result.value();
        List<CriticReport.Issue> issues = report.issues() == null ? List.of() : report.issues().stream()
                .filter(i -> i.document() != null)
                .filter(i -> switch (i.document()) {
                    case TAILORED_CV -> cv != null;
                    case COVER_LETTER -> letter != null;
                })
                .toList();
        return new LlmResult<>(new CriticReport(report.summary(), issues), result.model(), result.inputTokens(), result.outputTokens());
    }
}

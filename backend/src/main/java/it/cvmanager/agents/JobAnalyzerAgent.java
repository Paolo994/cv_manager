package it.cvmanager.agents;

import it.cvmanager.config.AppProperties;
import it.cvmanager.jobs.JobAnalysis;
import it.cvmanager.shared.llm.LlmGateway;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import org.springframework.stereotype.Component;

/** Testo di un annuncio -> requisiti, parole chiave, lingua. */
@Component
public class JobAnalyzerAgent {

    public static final String NAME = "job-analyzer";

    private final LlmGateway llm;
    private final AppProperties properties;
    private final String systemPrompt = Prompts.load(NAME);

    JobAnalyzerAgent(LlmGateway llm, AppProperties properties) {
        this.llm = llm;
        this.properties = properties;
    }

    public LlmResult<JobAnalysis> analyze(String jobDescription) {
        String user = "Analizza questo annuncio.\n\n" + Prompts.document("job_description", jobDescription);
        return llm.structured(LlmRequest.of(properties.agent(NAME), systemPrompt, user), JobAnalysis.class);
    }
}

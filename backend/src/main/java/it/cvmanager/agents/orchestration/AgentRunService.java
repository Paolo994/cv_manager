package it.cvmanager.agents.orchestration;

import it.cvmanager.shared.llm.LlmException;
import it.cvmanager.shared.llm.LlmResult;
import it.cvmanager.shared.web.ApiException;
import it.cvmanager.shared.web.NotFoundException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Orchestratore: esegue i workflow in background su virtual thread, salva run e passi
 * in DB e notifica ogni cambiamento di stato ai client SSE.
 * Il flusso è definito in codice (vedi WorkflowService), non deciso da un LLM.
 */
@Service
public class AgentRunService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunService.class);
    private static final String GENERIC_ERROR = "Si è verificato un errore inatteso durante l'elaborazione.";
    private static final String INTERRUPTED_ERROR = "Elaborazione interrotta dal riavvio del server. Riprova.";

    private final AgentRunRepository runs;
    private final AgentStepRepository steps;
    private final RunEventBroker broker;
    private final TransactionTemplate tx;
    private final JsonMapper jsonMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    AgentRunService(AgentRunRepository runs, AgentStepRepository steps, RunEventBroker broker,
                    PlatformTransactionManager transactionManager, JsonMapper jsonMapper) {
        this.runs = runs;
        this.steps = steps;
        this.broker = broker;
        this.tx = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    /**
     * Crea il run e avvia il corpo del workflow in background.
     * Il run è già salvato quando questo metodo restituisce l'id.
     */
    public UUID start(UUID userId, Workflow workflow, Object input, Function<RunContext, Object> body) {
        AgentRun run = tx.execute(status -> runs.save(new AgentRun(userId, workflow, jsonMapper.writeValueAsString(input))));
        UUID runId = run.getId();
        executor.submit(() -> execute(runId, userId, body));
        return runId;
    }

    public RunView view(UUID userId, UUID runId) {
        return tx.execute(status -> toView(runs.findByIdAndUserId(runId, userId).orElseThrow(() -> new NotFoundException("Run"))));
    }

    public SseEmitter subscribe(UUID userId, UUID runId) {
        view(userId, runId); // 404 se il run non è dell'utente
        return broker.subscribe(runId, () -> view(userId, runId));
    }

    private void execute(UUID runId, UUID userId, Function<RunContext, Object> body) {
        RunContext context = new RunContext(runId, userId, this);
        try {
            Object output = body.apply(context);
            String outputJson = output == null ? null : jsonMapper.writeValueAsString(output);
            updateRun(runId, run -> run.succeed(outputJson));
        } catch (Exception e) {
            String message = userMessage(e);
            log.warn("Run {} fallito: {}", runId, e.toString(), e instanceof LlmException || e instanceof ApiException ? null : e);
            updateRun(runId, run -> run.fail(message));
        }
    }

    <T> T executeStep(RunContext context, String agentName, Supplier<LlmResult<T>> agentCall) {
        UUID stepId = tx.execute(status -> steps.save(new AgentStep(context.runId(), agentName)).getId());
        publish(context.runId());
        try {
            LlmResult<T> result = agentCall.get();
            String outputJson = jsonMapper.writeValueAsString(result.value());
            tx.executeWithoutResult(status -> {
                steps.findById(stepId).orElseThrow()
                        .succeed(result.model(), outputJson, result.inputTokens(), result.outputTokens());
                runs.findById(context.runId()).orElseThrow().addTokens(result.inputTokens(), result.outputTokens());
            });
            publish(context.runId());
            return result.value();
        } catch (RuntimeException e) {
            String message = userMessage(e);
            tx.executeWithoutResult(status -> steps.findById(stepId).orElseThrow().fail(message));
            publish(context.runId());
            throw e;
        }
    }

    private void updateRun(UUID runId, java.util.function.Consumer<AgentRun> change) {
        tx.executeWithoutResult(status -> change.accept(runs.findById(runId).orElseThrow()));
        publish(runId);
    }

    private void publish(UUID runId) {
        RunView view = tx.execute(status -> runs.findById(runId).map(this::toView).orElse(null));
        if (view != null) {
            broker.publish(runId, view);
        }
    }

    private RunView toView(AgentRun run) {
        List<RunView.StepView> stepViews = steps.findByRunIdOrderByStartedAt(run.getId()).stream()
                .map(s -> new RunView.StepView(s.getAgent(), s.getModel(), s.getStatus(), s.getError(),
                        s.getInputTokens(), s.getOutputTokens(), s.getStartedAt(), s.getFinishedAt()))
                .toList();
        JsonNode output = run.getOutput() == null ? null : jsonMapper.readTree(run.getOutput());
        return new RunView(run.getId(), run.getWorkflow(), run.getStatus(), run.getError(), output,
                run.getInputTokens(), run.getOutputTokens(), run.getCreatedAt(), run.getFinishedAt(), stepViews);
    }

    /** Solo messaggi pensati per l'utente: i dettagli tecnici restano nei log. */
    private static String userMessage(Throwable e) {
        if (e instanceof LlmException || e instanceof ApiException) {
            return e.getMessage();
        }
        return GENERIC_ERROR;
    }

    /** I run rimasti "in corso" dopo un riavvio non verranno mai completati: li chiude come falliti. */
    @EventListener(ApplicationReadyEvent.class)
    void failInterruptedRuns() {
        tx.executeWithoutResult(status -> runs.findByStatus(RunStatus.RUNNING).forEach(run -> run.fail(INTERRUPTED_ERROR)));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

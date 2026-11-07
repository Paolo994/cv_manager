package it.cvmanager.agents.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Inoltra gli aggiornamenti dei run ai browser collegati via Server-Sent Events.
 * Ogni evento "update" contiene lo stato completo del run: il client sostituisce il proprio stato,
 * quindi un evento perso o duplicato non crea incoerenze.
 * Registro in memoria: va bene con una sola istanza dell'applicazione.
 */
@Component
class RunEventBroker {

    private static final Logger log = LoggerFactory.getLogger(RunEventBroker.class);
    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final Map<java.util.UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Registra il client prima di leggere lo stato iniziale: un aggiornamento che arriva nel mezzo
     * viene ricevuto comunque (al massimo due volte), mai perso.
     */
    SseEmitter subscribe(java.util.UUID runId, Supplier<RunView> snapshot) {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        List<SseEmitter> list = emitters.computeIfAbsent(runId, id -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> removeEmitter(runId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());

        send(runId, emitter, snapshot.get());
        return emitter;
    }

    void publish(java.util.UUID runId, RunView view) {
        List<SseEmitter> list = emitters.get(runId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            send(runId, emitter, view);
        }
    }

    private void send(java.util.UUID runId, SseEmitter emitter, RunView view) {
        try {
            emitter.send(SseEmitter.event().name("update").data(view, MediaType.APPLICATION_JSON));
            if (view.status().isTerminal()) {
                emitter.complete();
            }
        } catch (IOException | IllegalStateException e) {
            // client disconnesso o emitter già chiuso
            log.debug("Invio SSE per il run {} non riuscito: {}", runId, e.toString());
            removeEmitter(runId, emitter);
        }
    }

    private void removeEmitter(java.util.UUID runId, SseEmitter emitter) {
        emitters.computeIfPresent(runId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}

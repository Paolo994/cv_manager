package it.cvmanager.agents;

import it.cvmanager.documents.CriticReport;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/** Parte del messaggio con cui un agente riceve la propria bozza e i problemi segnalati dal Critic. */
final class Revisions {

    private Revisions() {
    }

    static void append(StringBuilder user, JsonMapper jsonMapper, Object previousDraft, List<CriticReport.Issue> feedback) {
        if (previousDraft == null || feedback == null || feedback.isEmpty()) {
            return;
        }
        user.append("\n\nQuesta è la tua versione precedente:\n\n")
                .append(Prompts.document("previous_draft", jsonMapper.writeValueAsString(previousDraft)))
                .append("\n\nIl Critic ha trovato questi problemi rispetto al profilo:\n\n")
                .append(Prompts.document("critic_feedback", jsonMapper.writeValueAsString(feedback)))
                .append("\n\nRiscrivi il documento correggendo tutti i problemi segnalati e mantenendo il resto. "
                        + "Se un'affermazione non è supportata dal profilo, eliminala o riformulala in modo fedele al profilo.");
    }
}

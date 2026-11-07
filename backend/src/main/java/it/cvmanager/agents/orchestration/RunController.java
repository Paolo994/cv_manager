package it.cvmanager.agents.orchestration;

import it.cvmanager.auth.AppUserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/runs")
class RunController {

    private final AgentRunService runService;

    RunController(AgentRunService runService) {
        this.runService = runService;
    }

    @GetMapping("/{id}")
    RunView get(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        return runService.view(user.id(), id);
    }

    /** Stream di eventi "update" con lo stato completo del run; si chiude quando il run termina. */
    @GetMapping(path = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        return runService.subscribe(user.id(), id);
    }
}

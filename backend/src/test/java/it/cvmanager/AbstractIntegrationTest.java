package it.cvmanager;

import it.cvmanager.shared.llm.LlmGateway;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dei test di integrazione: PostgreSQL reale (Testcontainers), filtri di Spring Session
 * e Spring Security come in produzione, LLM sostituito da un mock (nessuna chiamata a Claude).
 * Tutte le sottoclassi condividono lo stesso contesto Spring e lo stesso container.
 */
@SpringBootTest(properties = {
        "app.storage.local-dir=target/test-uploads",
        "spring.ai.anthropic.api-key=test-key-not-used"
})
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "password-sicura-123";

    @MockitoBean
    protected LlmGateway llm;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected JsonMapper jsonMapper;

    protected MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        // Filtro di Spring Session prima di Spring Security, come nell'app reale
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(SessionRepositoryFilter.class))
                .apply(springSecurity())
                .build();
    }

    protected static String uniqueEmail() {
        return "utente-" + UUID.randomUUID() + "@example.com";
    }

    protected static String registerJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s", "fullName": "Mario Rossi"}
                """.formatted(email, password);
    }

    /** Registra un nuovo utente, fa login e restituisce il cookie di sessione. */
    protected Cookie newLoggedInUser() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, PASSWORD)))
                .andExpect(status().isCreated());
        return mvc.perform(post("/api/auth/login").with(csrf()).param("email", email).param("password", PASSWORD))
                .andExpect(status().isNoContent())
                .andReturn().getResponse().getCookie("SESSION");
    }

    protected JsonNode json(String body) {
        return jsonMapper.readTree(body);
    }

    /** Attende la fine di un run asincrono e ne restituisce lo stato finale. */
    protected JsonNode awaitRun(Cookie session, String runId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(15));
        while (Instant.now().isBefore(deadline)) {
            String body = mvc.perform(get("/api/runs/" + runId).cookie(session))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode run = json(body);
            if (!"RUNNING".equals(run.get("status").asString())) {
                return run;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Il run " + runId + " non è terminato entro 15 secondi");
    }
}

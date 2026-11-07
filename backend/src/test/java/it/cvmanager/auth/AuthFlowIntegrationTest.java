package it.cvmanager.auth;

import it.cvmanager.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "password-sicura-123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserRepository users;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // Filtro di Spring Session prima di Spring Security, come nell'app reale:
        // la sessione viene salvata su PostgreSQL e restituita nel cookie SESSION.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(SessionRepositoryFilter.class))
                .apply(springSecurity())
                .build();
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerRequiresCsrfToken() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(uniqueEmail(), PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerValidatesInput() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("non-una-email", "corta")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerLoginMeLogout() throws Exception {
        String email = uniqueEmail();

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"));

        AppUser saved = users.findByEmail(email).orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain(PASSWORD);

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email, PASSWORD)))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email)
                        .param("password", "password-sbagliata"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email)
                        .param("password", PASSWORD))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie session = login.getResponse().getCookie("SESSION");
        assertThat(session).as("cookie di sessione").isNotNull();
        // Controllo sull'header reale ricevuto dal browser
        assertThat(login.getResponse().getHeaders("Set-Cookie"))
                .filteredOn(h -> h.startsWith("SESSION="))
                .singleElement()
                .satisfies(h -> assertThat(h).containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Lax"));

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Mario Rossi"));

        mvc.perform(post("/api/auth/logout").with(csrf()).cookie(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }

    private static String uniqueEmail() {
        return "utente-" + UUID.randomUUID() + "@example.com";
    }

    private static String registerJson(String email, String password) {
        return """
                {"email": "%s", "password": "%s", "fullName": "Mario Rossi"}
                """.formatted(email, password);
    }
}

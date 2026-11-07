package it.cvmanager.agents;

import it.cvmanager.AbstractIntegrationTest;
import it.cvmanager.TestDocuments;
import it.cvmanager.cv.review.CvReview;
import it.cvmanager.cv.review.CvReview.Criterion;
import it.cvmanager.cv.review.CvReview.CriterionScore;
import it.cvmanager.cv.review.CvReview.Priority;
import it.cvmanager.cv.review.CvReview.Suggestion;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.profile.CandidateProfile.Education;
import it.cvmanager.profile.CandidateProfile.Experience;
import it.cvmanager.profile.CandidateProfile.LanguageSkill;
import it.cvmanager.profile.Seniority;
import it.cvmanager.shared.llm.LlmException;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CvWorkflowIntegrationTest extends AbstractIntegrationTest {

    private static final CandidateProfile PARSED_PROFILE = new CandidateProfile(
            "Giulia Bianchi", "Senior Backend Developer", "Milano, Italia",
            "Sviluppatrice backend con 8 anni di esperienza su Java e Spring.",
            Seniority.SENIOR, 8,
            List.of(new Experience("Senior Backend Developer", "Acme Pagamenti S.p.A.", "Milano", "2021", "", true,
                    "Microservizi per i pagamenti online.", List.of("Ridotto la latenza media delle API del 35%"),
                    List.of("Java", "Spring Boot", "Redis"))),
            List.of(new Education("Laurea magistrale", "Ingegneria Informatica", "Politecnico di Torino", "", "2017", "108/110")),
            List.of("Java", "Spring Boot", "PostgreSQL"), List.of(),
            List.of(new LanguageSkill("Italiano", "Madrelingua"), new LanguageSkill("Inglese", "C1")),
            List.of(), "it");

    private static final CvReview REVIEW = new CvReview(150, "CV solido con risultati misurabili.",
            List.of(new CriterionScore(Criterion.IMPACT, 80, "Buoni risultati quantificati."),
                    new CriterionScore(Criterion.JOB_FIT, -10, "Fuori scala, da normalizzare.")),
            List.of("Risultati quantificati"),
            List.of(new Suggestion(Priority.HIGH, "Profilo", "Manca un sommario.", "Aggiungi 3 righe di sintesi.", "")));

    @Test
    void uploadImportEditAndConfirmProfile() throws Exception {
        Cookie session = newLoggedInUser();
        when(llm.structured(any(), eq(CandidateProfile.class)))
                .thenReturn(new LlmResult<>(PARSED_PROFILE, "claude-sonnet-5-5", 1200, 800));

        String cvId = uploadSampleCv(session);

        mvc.perform(get("/api/profile").cookie(session)).andExpect(status().isNotFound());

        String runId = startWorkflow(session, "import-cv", Map.of("cvId", cvId));
        JsonNode run = awaitRun(session, runId);
        assertThat(run.get("status").asString()).isEqualTo("SUCCEEDED");
        assertThat(run.get("workflow").asString()).isEqualTo("IMPORT_CV");
        assertThat(run.get("inputTokens").asInt()).isEqualTo(1200);
        assertThat(run.get("steps")).hasSize(1);
        assertThat(run.get("steps").get(0).get("agent").asString()).isEqualTo("cv-parser");
        assertThat(run.get("steps").get(0).get("status").asString()).isEqualTo("SUCCEEDED");

        // L'agente riceve il testo estratto dal PDF, delimitato come documento, con il modello configurato
        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm).structured(request.capture(), eq(CandidateProfile.class));
        assertThat(request.getValue().model()).isEqualTo("claude-sonnet-5-5");
        assertThat(request.getValue().user())
                .contains("<cv_text>", "</cv_text>", "Acme Pagamenti S.p.A.", "Ridotto la latenza media delle API del 35%");

        mvc.perform(get("/api/profile").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sourceCvId").value(cvId))
                .andExpect(jsonPath("$.profile.fullName").value("Giulia Bianchi"))
                .andExpect(jsonPath("$.profile.seniority").value("SENIOR"))
                .andExpect(jsonPath("$.profile.experiences[0].achievements[0]").value("Ridotto la latenza media delle API del 35%"));

        CandidateProfile edited = new CandidateProfile(PARSED_PROFILE.fullName(), "Senior Java Engineer",
                PARSED_PROFILE.location(), PARSED_PROFILE.summary(), Seniority.SENIOR, 8, PARSED_PROFILE.experiences(),
                PARSED_PROFILE.education(), List.of("Java", "Spring Boot", "Kafka"), List.of(), PARSED_PROFILE.languages(),
                List.of(), "it");
        mvc.perform(put("/api/profile").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("profile", edited, "confirm", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").isNotEmpty())
                .andExpect(jsonPath("$.profile.headline").value("Senior Java Engineer"))
                .andExpect(jsonPath("$.profile.technicalSkills[2]").value("Kafka"));

        // validazione: seniority obbligatoria
        String invalid = jsonMapper.writeValueAsString(Map.of("profile", Map.of("fullName", "X"), "confirm", true));
        mvc.perform(put("/api/profile").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewWithJobDescriptionIsSavedInHistory() throws Exception {
        Cookie session = newLoggedInUser();
        when(llm.structured(any(), eq(CvReview.class)))
                .thenReturn(new LlmResult<>(REVIEW, "claude-sonnet-5-5", 2000, 900));

        String cvId = uploadSampleCv(session);
        String runId = startWorkflow(session, "review-cv",
                Map.of("cvId", cvId, "jobDescription", "Cerchiamo un Senior Java Developer con esperienza su Kafka."));
        JsonNode run = awaitRun(session, runId);
        assertThat(run.get("status").asString()).isEqualTo("SUCCEEDED");
        assertThat(run.get("output").get("overallScore").asInt()).isEqualTo(100);

        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm).structured(request.capture(), eq(CvReview.class));
        assertThat(request.getValue().user()).contains("<job_description>", "Kafka", "JOB_FIT");
        assertThat(request.getValue().system()).contains("in italiano").doesNotContain("{language}");

        mvc.perform(get("/api/cvs/" + cvId + "/reviews").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].runId").value(runId))
                .andExpect(jsonPath("$[0].review.overallScore").value(100))
                .andExpect(jsonPath("$[0].review.criteria[1].score").value(0))
                .andExpect(jsonPath("$[0].jobDescription").value("Cerchiamo un Senior Java Developer con esperienza su Kafka."));
    }

    @Test
    void llmFailureMarksRunAsFailedWithReadableMessage() throws Exception {
        Cookie session = newLoggedInUser();
        when(llm.structured(any(), eq(CandidateProfile.class)))
                .thenThrow(new LlmException("Chiave API di Anthropic mancante o non valida.", null));

        String cvId = uploadSampleCv(session);
        JsonNode run = awaitRun(session, startWorkflow(session, "import-cv", Map.of("cvId", cvId)));

        assertThat(run.get("status").asString()).isEqualTo("FAILED");
        assertThat(run.get("error").asString()).isEqualTo("Chiave API di Anthropic mancante o non valida.");
        assertThat(run.get("steps").get(0).get("status").asString()).isEqualTo("FAILED");
        mvc.perform(get("/api/profile").cookie(session)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedAndEmptyFiles() throws Exception {
        Cookie session = newLoggedInUser();

        // estensione .pdf ma contenuto di testo: il tipo si riconosce dal contenuto
        MockMultipartFile fakePdf = new MockMultipartFile("file", "cv.pdf", "application/pdf",
                "solo testo, non un pdf".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/cvs").file(fakePdf).with(csrf()).cookie(session))
                .andExpect(status().isUnsupportedMediaType());

        // PDF valido senza testo (es. scansione)
        MockMultipartFile emptyPdf = new MockMultipartFile("file", "scansione.pdf", "application/pdf",
                TestDocuments.pdf(List.of()));
        mvc.perform(multipart("/api/cvs").file(emptyPdf).with(csrf()).cookie(session))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Testo non trovato"));
    }

    @Test
    void usersCannotAccessEachOthersData() throws Exception {
        Cookie owner = newLoggedInUser();
        Cookie other = newLoggedInUser();
        when(llm.structured(any(), eq(CandidateProfile.class)))
                .thenReturn(new LlmResult<>(PARSED_PROFILE, "claude-sonnet-5-5", 10, 10));

        String cvId = uploadSampleCv(owner);
        String runId = startWorkflow(owner, "import-cv", Map.of("cvId", cvId));
        awaitRun(owner, runId);

        mvc.perform(get("/api/cvs/" + cvId).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/cvs/" + cvId + "/file").cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/cvs/" + cvId + "/reviews").cookie(other)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/cvs/" + cvId).with(csrf()).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/runs/" + runId).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/runs/" + runId + "/events").cookie(other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/workflows/import-cv").with(csrf()).cookie(other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("cvId", cvId))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/cvs").cookie(other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/profile").cookie(other)).andExpect(status().isNotFound());
    }

    @Test
    void downloadAndDeleteCv() throws Exception {
        Cookie session = newLoggedInUser();
        byte[] pdf = TestDocuments.sampleCvPdf();
        String cvId = json(mvc.perform(multipart("/api/cvs")
                        .file(new MockMultipartFile("file", "Il mio CV.pdf", "application/pdf", pdf))
                        .with(csrf()).cookie(session))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asString();

        mvc.perform(get("/api/cvs/" + cvId + "/file").cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));

        Path uploads = Path.of("target/test-uploads");
        long filesBefore = countFiles(uploads);
        mvc.perform(delete("/api/cvs/" + cvId).with(csrf()).cookie(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/cvs/" + cvId).cookie(session)).andExpect(status().isNotFound());
        assertThat(countFiles(uploads)).isEqualTo(filesBefore - 1);
    }

    @Test
    void preferencesHaveDefaultsAndAreValidated() throws Exception {
        Cookie session = newLoggedInUser();

        mvc.perform(get("/api/preferences").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationLanguages[0]").value("IT"))
                .andExpect(jsonPath("$.targetRoles.length()").value(0));

        String preferences = """
                {"targetRoles": ["Backend Developer"], "locations": ["Milano"], "workModes": ["REMOTE", "HYBRID"],
                 "minAnnualSalaryEur": 45000, "seniority": "SENIOR", "excludedIndustries": ["Gambling"],
                 "applicationLanguages": ["IT", "EN"], "willingToRelocate": false}
                """;
        mvc.perform(put("/api/preferences").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(preferences))
                .andExpect(status().isOk());
        mvc.perform(get("/api/preferences").cookie(session))
                .andExpect(jsonPath("$.workModes[1]").value("HYBRID"))
                .andExpect(jsonPath("$.minAnnualSalaryEur").value(45000))
                .andExpect(jsonPath("$.applicationLanguages.length()").value(2));

        mvc.perform(put("/api/preferences").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(preferences.replace("45000", "-5")))
                .andExpect(status().isBadRequest());
    }

    private String uploadSampleCv(Cookie session) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", TestDocuments.sampleCvPdf());
        String body = mvc.perform(multipart("/api/cvs").file(file).with(csrf()).cookie(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andReturn().getResponse().getContentAsString();
        JsonNode cv = json(body);
        assertThat(cv.get("textLength").asInt()).isGreaterThan(300);
        return cv.get("id").asString();
    }

    private String startWorkflow(Cookie session, String workflow, Map<String, Object> body) throws Exception {
        String response = mvc.perform(post("/api/workflows/" + workflow).with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return json(response).get("runId").asString();
    }

    private static long countFiles(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (var files = Files.walk(dir)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}

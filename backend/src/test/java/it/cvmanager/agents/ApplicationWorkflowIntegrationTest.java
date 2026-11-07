package it.cvmanager.agents;

import it.cvmanager.AbstractIntegrationTest;
import it.cvmanager.TestDocuments;
import it.cvmanager.documents.CoverLetter;
import it.cvmanager.documents.CriticReport;
import it.cvmanager.documents.CriticReport.Issue;
import it.cvmanager.documents.CriticReport.IssueType;
import it.cvmanager.documents.DocumentType;
import it.cvmanager.documents.TailoredCv;
import it.cvmanager.jobs.JobAnalysis;
import it.cvmanager.profile.CandidateProfile;
import it.cvmanager.profile.Seniority;
import it.cvmanager.shared.Language;
import it.cvmanager.shared.llm.LlmRequest;
import it.cvmanager.shared.llm.LlmResult;
import jakarta.servlet.http.Cookie;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationWorkflowIntegrationTest extends AbstractIntegrationTest {

    private static final String PHONE = "+39 333 1234567";

    private static final String JOB_TEXT = """
            Acme Pagamenti cerca un Senior Backend Engineer per la piattaforma di pagamenti online.
            Requisiti: 5+ anni con Java e Spring Boot, PostgreSQL, esperienza con Kafka. Gradito Kubernetes.
            Sede: Milano, modalità ibrida.
            """;

    private static final CandidateProfile PROFILE = new CandidateProfile(
            "Giulia Bianchi", "Senior Backend Developer", "Milano, Italia", "Sviluppatrice backend.",
            Seniority.SENIOR, 8,
            List.of(new CandidateProfile.Experience("Senior Backend Developer", "Software Nord", "Milano", "2018-01", "", true,
                    "Microservizi Java.", List.of("Ridotto la latenza delle API del 35%"), List.of("Java", "Spring Boot"))),
            List.of(), List.of("Java", "Spring Boot", "PostgreSQL"), List.of(), List.of(), List.of(), "it");

    private static final JobAnalysis ANALYSIS = new JobAnalysis("Senior Backend Engineer", "Acme Pagamenti", "Milano",
            JobAnalysis.WorkMode.HYBRID, "SENIOR", Language.IT, List.of("5+ anni con Java", "Kafka"), List.of("Kubernetes"),
            List.of("Sviluppo della piattaforma di pagamenti"), List.of("Java", "Spring Boot", "Kafka"), "", "Ruolo backend senior.");

    private static final TailoredCv CV = new TailoredCv("Giulia Bianchi", "Senior Backend Engineer", "Otto anni su Java.",
            List.of(new TailoredCv.Experience("Senior Backend Developer", "Software Nord", "Milano", "01/2018 - oggi",
                    List.of("Ridotto la latenza delle API del 35%", "Sviluppato microservizi con Spring Boot → PostgreSQL 🚀"))),
            List.of(), List.of(new TailoredCv.SkillGroup("Backend", List.of("Java", "Spring Boot", "PostgreSQL"))),
            List.of("Inglese - C1"), List.of(), List.of("Messi in evidenza i microservizi.", "Kafka non risulta dal profilo."));

    private static final CoverLetter LETTER_WITH_INVENTION = new CoverLetter("Candidatura Senior Backend Engineer",
            "Gentile team di selezione di Acme Pagamenti,",
            List.of("Ho cinque anni di esperienza con Kafka.", "Ho ridotto la latenza delle API del 35%."), "Cordiali saluti");

    private static final CoverLetter LETTER_FIXED = new CoverLetter("Candidatura Senior Backend Engineer",
            "Gentile team di selezione di Acme Pagamenti,",
            List.of("Mi candido per il ruolo di Senior Backend Engineer.", "Ho ridotto la latenza delle API del 35%."),
            "Cordiali saluti");

    private static final CriticReport BLOCKING_ON_LETTER = new CriticReport("Una affermazione non supportata.",
            List.of(new Issue(DocumentType.COVER_LETTER, IssueType.UNSUPPORTED_CLAIM, "Ho cinque anni di esperienza con Kafka.",
                    "Kafka non è nel profilo.", "Elimina la frase.")));

    private static final CriticReport CLEAN = new CriticReport("Documenti fedeli al profilo.", List.of());

    @Test
    void criticSendsBackOnlyTheLetterAndDocumentsAreSaved() throws Exception {
        Cookie session = userWithConfirmedProfile();
        mockDrafts();
        when(llm.structured(any(), eq(CoverLetter.class))).thenReturn(result(LETTER_WITH_INVENTION), result(LETTER_FIXED));
        when(llm.structured(any(), eq(CriticReport.class))).thenReturn(result(BLOCKING_ON_LETTER), result(CLEAN));

        String jobId = createJob(session);
        JsonNode run = awaitRun(session, prepare(session, jobId, List.of("TAILORED_CV", "COVER_LETTER")));

        assertThat(run.get("status").asString()).isEqualTo("SUCCEEDED");
        assertThat(run.get("output").get("revisions").asInt()).isEqualTo(1);
        assertThat(run.get("output").get("blockingIssues").asBoolean()).isFalse();
        List<String> steps = run.get("steps").valueStream()
                .map(s -> s.get("agent").asString() + "#" + s.get("attempt").asInt()).toList();
        assertThat(steps).hasSize(6).startsWith("job-analyzer#1")
                .contains("cv-tailor#1", "cover-letter-writer#1", "critic#1", "cover-letter-writer#2", "critic#2")
                .doesNotContain("cv-tailor#2");

        // la revisione riceve la bozza precedente e il feedback del Critic
        ArgumentCaptor<LlmRequest> letterRequests = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm, times(2)).structured(letterRequests.capture(), eq(CoverLetter.class));
        assertThat(letterRequests.getAllValues().get(0).user()).doesNotContain("<critic_feedback>");
        assertThat(letterRequests.getAllValues().get(1).user())
                .contains("<previous_draft>", "<critic_feedback>", "Kafka non è nel profilo.");
        assertThat(letterRequests.getAllValues().get(1).model()).isEqualTo("claude-opus-5-5");

        mvc.perform(get("/api/jobs/" + jobId).cookie(session))
                .andExpect(jsonPath("$.analysis.company").value("Acme Pagamenti"));
        mvc.perform(get("/api/jobs/" + jobId + "/documents").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.type == 'COVER_LETTER')].content.paragraphs[0]").value("Mi candido per il ruolo di Senior Backend Engineer."))
                .andExpect(jsonPath("$[?(@.type == 'COVER_LETTER')].reviewNotes.length()").value(0))
                .andExpect(jsonPath("$[?(@.type == 'TAILORED_CV')].version").value(1));
    }

    @Test
    void stopsAfterTwoRevisionsAndKeepsOpenIssuesVisible() throws Exception {
        Cookie session = userWithConfirmedProfile();
        mockDrafts();
        when(llm.structured(any(), eq(CoverLetter.class))).thenReturn(result(LETTER_WITH_INVENTION));
        when(llm.structured(any(), eq(CriticReport.class))).thenReturn(result(BLOCKING_ON_LETTER));

        String jobId = createJob(session);
        JsonNode run = awaitRun(session, prepare(session, jobId, List.of("COVER_LETTER")));

        assertThat(run.get("status").asString()).isEqualTo("SUCCEEDED");
        assertThat(run.get("output").get("revisions").asInt()).isEqualTo(2);
        assertThat(run.get("output").get("blockingIssues").asBoolean()).isTrue();
        verify(llm, times(3)).structured(any(), eq(CoverLetter.class));
        verify(llm, times(3)).structured(any(), eq(CriticReport.class));
        verify(llm, times(0)).structured(any(), eq(TailoredCv.class));

        mvc.perform(get("/api/jobs/" + jobId + "/documents").cookie(session))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewNotes[0].type").value("UNSUPPORTED_CLAIM"));
    }

    @Test
    void requiresConfirmedProfile() throws Exception {
        Cookie session = newLoggedInUser();
        String jobId = createJob(session);
        mvc.perform(post("/api/workflows/prepare-application").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("jobId", jobId, "documents", List.of("COVER_LETTER")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Profilo da confermare"));
    }

    @Test
    void exportsPdfAndDocxWithContactsThatNeverReachTheModel() throws Exception {
        Cookie session = userWithConfirmedProfile();
        mockDrafts();
        when(llm.structured(any(), eq(CoverLetter.class))).thenReturn(result(LETTER_FIXED));
        when(llm.structured(any(), eq(CriticReport.class))).thenReturn(result(CLEAN));

        mvc.perform(put("/api/profile/contact").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("email", "giulia@example.com", "phone", PHONE,
                                "linkedin", "linkedin.com/in/giulia", "website", "", "city", "Milano"))))
                .andExpect(status().isOk());

        String jobId = createJob(session);
        awaitRun(session, prepare(session, jobId, List.of("TAILORED_CV", "COVER_LETTER")));
        JsonNode documents = json(mvc.perform(get("/api/jobs/" + jobId + "/documents").cookie(session))
                .andReturn().getResponse().getContentAsString());
        String cvId = documentId(documents, "TAILORED_CV");
        String letterId = documentId(documents, "COVER_LETTER");

        // PDF del CV: caratteri non supportati dal font (emoji, frecce) sostituiti, contatti in intestazione
        byte[] pdf = mvc.perform(get("/api/documents/" + cvId + "/export?format=pdf").cookie(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("cv-giulia-bianchi-acme-pagamenti.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Giulia Bianchi", "ESPERIENZA PROFESSIONALE", PHONE, "Ridotto la latenza delle API del 35%");
        }

        // DOCX della lettera: data, azienda, oggetto, firma
        byte[] docx = mvc.perform(get("/api/documents/" + letterId + "/export?format=docx").cookie(session))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            assertThat(extractor.getText()).contains("Oggetto: Candidatura Senior Backend Engineer", "Acme Pagamenti",
                    "Cordiali saluti", "Giulia Bianchi", "giulia@example.com");
        }

        mvc.perform(get("/api/documents/" + letterId + "/export?format=exe").cookie(session)).andExpect(status().isBadRequest());

        // i contatti servono solo all'esportazione: nessun prompt li contiene
        ArgumentCaptor<LlmRequest> requests = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm, atLeastOnce()).structured(requests.capture(), any());
        assertThat(requests.getAllValues()).allSatisfy(r -> assertThat(r.user()).doesNotContain(PHONE, "giulia@example.com"));
    }

    @Test
    void userEditOfLetterCreatesNewVersion() throws Exception {
        Cookie session = userWithConfirmedProfile();
        mockDrafts();
        when(llm.structured(any(), eq(CoverLetter.class))).thenReturn(result(LETTER_FIXED));
        when(llm.structured(any(), eq(CriticReport.class))).thenReturn(result(CLEAN));
        String jobId = createJob(session);
        awaitRun(session, prepare(session, jobId, List.of("TAILORED_CV", "COVER_LETTER")));
        JsonNode documents = json(mvc.perform(get("/api/jobs/" + jobId + "/documents").cookie(session))
                .andReturn().getResponse().getContentAsString());

        CoverLetter edited = new CoverLetter("Candidatura", "Gentili,", List.of("Paragrafo riscritto da me."), "Saluti");
        mvc.perform(post("/api/documents/" + documentId(documents, "COVER_LETTER") + "/revisions").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(edited)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.origin").value("USER"));

        mvc.perform(post("/api/documents/" + documentId(documents, "TAILORED_CV") + "/revisions").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(edited)))
                .andExpect(status().isBadRequest());

        CoverLetter empty = new CoverLetter("", "", List.of(), "");
        mvc.perform(post("/api/documents/" + documentId(documents, "COVER_LETTER") + "/revisions").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(empty)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void jobsAndDocumentsAreIsolatedBetweenUsers() throws Exception {
        Cookie owner = userWithConfirmedProfile();
        Cookie other = userWithConfirmedProfile();
        mockDrafts();
        when(llm.structured(any(), eq(CoverLetter.class))).thenReturn(result(LETTER_FIXED));
        when(llm.structured(any(), eq(CriticReport.class))).thenReturn(result(CLEAN));
        String jobId = createJob(owner);
        awaitRun(owner, prepare(owner, jobId, List.of("COVER_LETTER")));
        String letterId = documentId(json(mvc.perform(get("/api/jobs/" + jobId + "/documents").cookie(owner))
                .andReturn().getResponse().getContentAsString()), "COVER_LETTER");

        mvc.perform(get("/api/jobs/" + jobId).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/jobs/" + jobId + "/documents").cookie(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/" + letterId + "/export").cookie(other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/documents/" + letterId + "/revisions").with(csrf()).cookie(other)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(LETTER_FIXED)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/workflows/prepare-application").with(csrf()).cookie(other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("jobId", jobId, "documents", List.of("COVER_LETTER")))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/jobs").cookie(other)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void validatesJobPostings() throws Exception {
        Cookie session = newLoggedInUser();
        mvc.perform(post("/api/jobs").with(csrf()).cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("description", "troppo corto"))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/jobs").with(csrf()).cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("description", JOB_TEXT, "url", "javascript:alert(1)"))))
                .andExpect(status().isBadRequest());
    }

    // --- helper ---

    private void mockDrafts() {
        when(llm.structured(any(), eq(JobAnalysis.class))).thenReturn(result(ANALYSIS));
        when(llm.structured(any(), eq(TailoredCv.class))).thenReturn(result(CV));
    }

    private Cookie userWithConfirmedProfile() throws Exception {
        Cookie session = newLoggedInUser();
        when(llm.structured(any(), eq(CandidateProfile.class))).thenReturn(result(PROFILE));
        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", TestDocuments.sampleCvPdf());
        String cvId = json(mvc.perform(multipart("/api/cvs").file(file).with(csrf()).cookie(session))
                .andReturn().getResponse().getContentAsString()).get("id").asString();
        String runId = json(mvc.perform(post("/api/workflows/import-cv").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(Map.of("cvId", cvId))))
                .andReturn().getResponse().getContentAsString()).get("runId").asString();
        awaitRun(session, runId);
        mvc.perform(put("/api/profile").with(csrf()).cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("profile", PROFILE, "confirm", true))))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        return session;
    }

    private String createJob(Cookie session) throws Exception {
        String body = mvc.perform(post("/api/jobs").with(csrf()).cookie(session).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("description", JOB_TEXT, "url", "https://example.com/annuncio"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.analysis").isEmpty())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("id").asString();
    }

    private String prepare(Cookie session, String jobId, List<String> documents) throws Exception {
        String body = mvc.perform(post("/api/workflows/prepare-application").with(csrf()).cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("jobId", jobId, "documents", documents, "tone", "PROFESSIONAL"))))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return json(body).get("runId").asString();
    }

    private static String documentId(JsonNode documents, String type) {
        return documents.valueStream().filter(d -> d.get("type").asString().equals(type)).findFirst().orElseThrow()
                .get("id").asString();
    }

    private static <T> LlmResult<T> result(T value) {
        return new LlmResult<>(value, "test-model", 100, 50);
    }
}

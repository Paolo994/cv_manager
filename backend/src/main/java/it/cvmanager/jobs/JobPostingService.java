package it.cvmanager.jobs;

import it.cvmanager.shared.web.ApiException;
import it.cvmanager.shared.web.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class JobPostingService {

    private final JobPostingRepository postings;
    private final JsonMapper jsonMapper;

    JobPostingService(JobPostingRepository postings, JsonMapper jsonMapper) {
        this.postings = postings;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public JobPostingView createManual(UUID userId, String description, String url) {
        return toView(postings.save(JobPosting.manual(userId, description.strip(), normalizeUrl(url))));
    }

    @Transactional(readOnly = true)
    public List<JobPostingView> list(UUID userId) {
        return postings.findByOwnerUserIdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public JobPostingView get(UUID userId, UUID jobId) {
        return toView(find(userId, jobId));
    }

    @Transactional
    public void saveAnalysis(UUID userId, UUID jobId, JobAnalysis analysis) {
        find(userId, jobId).setAnalysis(jsonMapper.writeValueAsString(analysis));
    }

    @Transactional
    public void delete(UUID userId, UUID jobId) {
        postings.delete(find(userId, jobId));
    }

    private JobPosting find(UUID userId, UUID jobId) {
        return postings.findByIdAndOwnerUserId(jobId, userId).orElseThrow(() -> new NotFoundException("Annuncio"));
    }

    private JobPostingView toView(JobPosting p) {
        JobAnalysis analysis = p.getAnalysis() == null ? null : jsonMapper.readValue(p.getAnalysis(), JobAnalysis.class);
        return new JobPostingView(p.getId(), p.getSource().name(), p.getUrl(), p.getDescription(), analysis, p.getCreatedAt());
    }

    /** Solo link http(s): l'URL viene mostrato come collegamento nella SPA. */
    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.strip();
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null) {
                return trimmed;
            }
        } catch (IllegalArgumentException ignored) {
            // gestito sotto
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Link non valido", "Il link dell'annuncio deve iniziare con http:// o https://");
    }
}

package it.cvmanager.cv;

import it.cvmanager.auth.AppUserPrincipal;
import it.cvmanager.cv.dto.CvDetail;
import it.cvmanager.cv.dto.CvSummary;
import it.cvmanager.cv.review.CvReviewService;
import it.cvmanager.cv.review.CvReviewService.ReviewView;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cvs")
class CvController {

    private final CvService cvService;
    private final CvReviewService reviewService;

    CvController(CvService cvService, CvReviewService reviewService) {
        this.cvService = cvService;
        this.reviewService = reviewService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    CvSummary upload(@AuthenticationPrincipal AppUserPrincipal user, @RequestParam("file") MultipartFile file) {
        return cvService.upload(user.id(), file);
    }

    @GetMapping
    List<CvSummary> list(@AuthenticationPrincipal AppUserPrincipal user) {
        return cvService.list(user.id());
    }

    @GetMapping("/{id}")
    CvDetail get(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        return cvService.get(user.id(), id);
    }

    @GetMapping("/{id}/file")
    ResponseEntity<byte[]> download(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        CvService.StoredFile file = cvService.file(user.id(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        cvService.delete(user.id(), id);
    }

    @GetMapping("/{id}/reviews")
    List<ReviewView> reviews(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id) {
        return reviewService.list(user.id(), id);
    }
}

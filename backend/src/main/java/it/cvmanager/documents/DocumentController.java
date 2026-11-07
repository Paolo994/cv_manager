package it.cvmanager.documents;

import it.cvmanager.auth.AppUserPrincipal;
import it.cvmanager.documents.export.DocumentExporter;
import it.cvmanager.documents.export.DocumentExporter.ExportedFile;
import it.cvmanager.documents.export.DocumentExporter.Format;
import it.cvmanager.shared.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
class DocumentController {

    private final DocumentService documents;
    private final DocumentExporter exporter;

    DocumentController(DocumentService documents, DocumentExporter exporter) {
        this.documents = documents;
        this.exporter = exporter;
    }

    /** Tutte le versioni dei documenti dell'annuncio, per tipo e dalla più recente. */
    @GetMapping("/api/jobs/{jobId}/documents")
    List<DocumentView> list(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID jobId) {
        return documents.listForJob(user.id(), jobId);
    }

    /** L'utente modifica una lettera: viene salvata come nuova versione. */
    @PostMapping("/api/documents/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    DocumentView revise(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id,
                        @Valid @RequestBody CoverLetter letter) {
        return documents.saveUserEdit(user.id(), id, letter);
    }

    @GetMapping("/api/documents/{id}/export")
    ResponseEntity<byte[]> export(@AuthenticationPrincipal AppUserPrincipal user, @PathVariable UUID id,
                                  @RequestParam(defaultValue = "pdf") String format) {
        Format parsed;
        try {
            parsed = Format.valueOf(format.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Formato non valido", "Formati disponibili: pdf, docx.");
        }
        ExportedFile file = exporter.export(user.id(), user.email(), id, parsed);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }
}

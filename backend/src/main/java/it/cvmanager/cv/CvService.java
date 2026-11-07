package it.cvmanager.cv;

import it.cvmanager.cv.dto.CvDetail;
import it.cvmanager.cv.dto.CvSummary;
import it.cvmanager.shared.storage.FileStorage;
import it.cvmanager.shared.web.ApiException;
import it.cvmanager.shared.web.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class CvService {

    private static final int MAX_FILENAME_LENGTH = 255;

    private final CvDocumentRepository documents;
    private final CvTextExtractor extractor;
    private final FileStorage storage;

    CvService(CvDocumentRepository documents, CvTextExtractor extractor, FileStorage storage) {
        this.documents = documents;
        this.extractor = extractor;
        this.storage = storage;
    }

    @Transactional
    public CvSummary upload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File mancante", "Seleziona un file da caricare.");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Upload non riuscito", "Il file non è stato ricevuto correttamente.");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        CvTextExtractor.ExtractedCv extracted = extractor.extract(content, filename);

        String key = storage.store(userId.toString(), extracted.extension(), content);
        CvDocument document = new CvDocument(userId, filename, extracted.mediaType(), content.length, key, extracted.text());
        return CvSummary.of(documents.save(document));
    }

    @Transactional(readOnly = true)
    public List<CvSummary> list(UUID userId) {
        return documents.findByUserIdOrderByCreatedAtDesc(userId).stream().map(CvSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public CvDetail get(UUID userId, UUID cvId) {
        return CvDetail.of(find(userId, cvId));
    }

    /** Testo estratto, usato dagli agenti: verifica che il CV appartenga all'utente. */
    @Transactional(readOnly = true)
    public String extractedText(UUID userId, UUID cvId) {
        return find(userId, cvId).getExtractedText();
    }

    @Transactional(readOnly = true)
    public StoredFile file(UUID userId, UUID cvId) {
        CvDocument document = find(userId, cvId);
        return new StoredFile(document.getOriginalFilename(), document.getContentType(), storage.read(document.getStorageKey()));
    }

    @Transactional
    public void delete(UUID userId, UUID cvId) {
        CvDocument document = find(userId, cvId);
        documents.delete(document);
        documents.flush();
        storage.delete(document.getStorageKey());
    }

    private CvDocument find(UUID userId, UUID cvId) {
        return documents.findByIdAndUserId(cvId, userId).orElseThrow(() -> new NotFoundException("CV"));
    }

    private static String sanitizeFilename(String original) {
        String name = original == null ? "" : original.replaceAll("[\\\\/]", "_").replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            name = "cv";
        }
        return name.length() > MAX_FILENAME_LENGTH ? name.substring(name.length() - MAX_FILENAME_LENGTH) : name;
    }

    public record StoredFile(String filename, String contentType, byte[] content) {
    }
}

package it.cvmanager.cv.dto;

import it.cvmanager.cv.CvDocument;

import java.time.Instant;
import java.util.UUID;

public record CvSummary(UUID id, String filename, String contentType, long sizeBytes, int textLength, Instant createdAt) {

    public static CvSummary of(CvDocument document) {
        return new CvSummary(document.getId(), document.getOriginalFilename(), document.getContentType(),
                document.getSizeBytes(), document.getExtractedText().length(), document.getCreatedAt());
    }
}

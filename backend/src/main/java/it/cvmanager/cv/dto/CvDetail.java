package it.cvmanager.cv.dto;

import it.cvmanager.cv.CvDocument;

import java.time.Instant;
import java.util.UUID;

public record CvDetail(UUID id, String filename, String contentType, long sizeBytes, Instant createdAt, String extractedText) {

    public static CvDetail of(CvDocument document) {
        return new CvDetail(document.getId(), document.getOriginalFilename(), document.getContentType(),
                document.getSizeBytes(), document.getCreatedAt(), document.getExtractedText());
    }
}

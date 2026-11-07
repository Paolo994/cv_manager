package it.cvmanager.cv;

import it.cvmanager.shared.web.ApiException;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Riconosce il tipo reale del file dal contenuto (non dall'estensione) ed estrae il testo con Apache Tika.
 */
@Component
class CvTextExtractor {

    static final Map<String, String> SUPPORTED_TYPES = Map.of(
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    /** Un CV ha almeno qualche riga di testo: sotto questa soglia è probabilmente un PDF scansionato. */
    private static final int MIN_TEXT_LENGTH = 100;
    private static final int MAX_TEXT_LENGTH = 100_000;

    private final Tika tika = new Tika();

    ExtractedCv extract(byte[] content, String filename) {
        String mediaType = tika.detect(content, filename);
        String extension = SUPPORTED_TYPES.get(mediaType);
        if (extension == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato non supportato",
                    "Carica il CV in formato PDF o DOCX.");
        }
        String text;
        try {
            tika.setMaxStringLength(MAX_TEXT_LENGTH);
            text = tika.parseToString(new ByteArrayInputStream(content));
        } catch (IOException | TikaException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "File non leggibile",
                    "Non è stato possibile leggere il file: potrebbe essere danneggiato o protetto da password.");
        }
        String normalized = normalize(text);
        if (normalized.length() < MIN_TEXT_LENGTH) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Testo non trovato",
                    "Il file non contiene testo selezionabile. Se è una scansione, esportalo di nuovo come PDF di testo.");
        }
        return new ExtractedCv(mediaType, extension, normalized);
    }

    /** Spazi uniformi e al massimo una riga vuota consecutiva. */
    static String normalize(String text) {
        return text.replace("\r\n", "\n")
                .replace(' ', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    record ExtractedCv(String mediaType, String extension, String text) {
    }
}

package it.cvmanager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** PDF generati nei test: niente file binari nel repository. */
public final class TestDocuments {

    public static final List<String> SAMPLE_CV_LINES = List.of(
            "Giulia Bianchi",
            "Senior Backend Developer - Milano, Italia",
            "",
            "ESPERIENZA",
            "2021 - oggi  Senior Backend Developer, Acme Pagamenti S.p.A., Milano",
            "Progettazione di microservizi Java 21 e Spring Boot per i pagamenti online.",
            "Ridotto la latenza media delle API del 35% introducendo caching con Redis.",
            "2017 - 2021  Backend Developer, Software Nord S.r.l., Torino",
            "Sviluppo di API REST con Spring e PostgreSQL per clienti del settore retail.",
            "",
            "FORMAZIONE",
            "2017  Laurea magistrale in Ingegneria Informatica, Politecnico di Torino, 108/110",
            "",
            "COMPETENZE",
            "Java, Spring Boot, PostgreSQL, Redis, Docker, Kubernetes, AWS",
            "",
            "LINGUE",
            "Italiano madrelingua, Inglese C1");

    private TestDocuments() {
    }

    public static byte[] pdf(List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                content.newLineAtOffset(50, 740);
                for (String line : lines) {
                    content.showText(line);
                    content.newLineAtOffset(0, -16);
                }
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    public static byte[] sampleCvPdf() throws IOException {
        return pdf(SAMPLE_CV_LINES);
    }
}

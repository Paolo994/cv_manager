package it.cvmanager.documents.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Impagina i blocchi in un PDF A4 con PDFBox e i font standard (Helvetica): nessun font di sistema,
 * quindi stesso risultato su ogni macchina. I caratteri fuori da WinAnsi vengono sostituiti.
 */
final class PdfRenderer {

    private static final float MARGIN = 56;
    private static final float BULLET_INDENT = 12;
    private static final Color TEXT = new Color(0x1c, 0x1f, 0x24);
    private static final Color MUTED = new Color(0x5d, 0x65, 0x73);
    private static final Color ACCENT = new Color(0x2f, 0x6f, 0xdf);

    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDFont italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private PDDocument document;
    private PDPageContentStream content;
    private float y;
    private float width;

    static byte[] render(List<Block> blocks, String title) {
        return new PdfRenderer().draw(blocks, title);
    }

    private byte[] draw(List<Block> blocks, String title) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            this.document = doc;
            doc.getDocumentInformation().setTitle(title);
            doc.getDocumentInformation().setCreator("CV Manager");
            newPage();
            for (Block block : blocks) {
                drawBlock(block);
            }
            content.close();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Generazione del PDF non riuscita", e);
        }
    }

    private void drawBlock(Block block) throws IOException {
        switch (block) {
            case Block.Title t -> lines(t.text(), bold, 20, TEXT, 0, 6);
            case Block.Subtitle s -> lines(s.text(), regular, 12, ACCENT, 0, 2);
            case Block.Note n -> lines(n.text(), italic, 9.5f, MUTED, 0, 2);
            case Block.Paragraph p -> lines(p.text(), p.bold() ? bold : regular, 10.5f, TEXT, 0, 2);
            case Block.Bullet b -> bullet(b.text());
            case Block.Space s -> y -= s.points();
            case Block.Section s -> section(s.text());
            case Block.Entry e -> entry(e.text(), e.right());
        }
    }

    private void section(String text) throws IOException {
        ensureSpace(40);
        y -= 12;
        lines(text.toUpperCase(), bold, 11, ACCENT, 0, 0);
        y -= 3;
        content.setStrokingColor(ACCENT);
        content.setLineWidth(0.8f);
        content.moveTo(MARGIN, y);
        content.lineTo(MARGIN + width, y);
        content.stroke();
        y -= 8;
    }

    private void entry(String left, String right) throws IOException {
        ensureSpace(30);
        float size = 10.5f;
        String safeRight = safe(right, regular);
        float rightWidth = safeRight.isEmpty() ? 0 : regular.getStringWidth(safeRight) / 1000 * size;
        float leftWidth = width - rightWidth - (rightWidth > 0 ? 12 : 0);
        List<String> wrapped = wrap(safe(left, bold), bold, size, leftWidth);
        for (int i = 0; i < wrapped.size(); i++) {
            ensureSpace(size * 1.4f);
            y -= size * 1.35f;
            text(wrapped.get(i), bold, size, TEXT, MARGIN);
            if (i == 0 && rightWidth > 0) {
                text(safeRight, regular, size, MUTED, MARGIN + width - rightWidth);
            }
        }
        y -= 2;
    }

    private void bullet(String text) throws IOException {
        float size = 10.5f;
        List<String> wrapped = wrap(safe(text, regular), regular, size, width - BULLET_INDENT);
        for (int i = 0; i < wrapped.size(); i++) {
            ensureSpace(size * 1.4f);
            y -= size * 1.35f;
            if (i == 0) {
                text("•", regular, size, ACCENT, MARGIN + 2);
            }
            text(wrapped.get(i), regular, size, TEXT, MARGIN + BULLET_INDENT);
        }
        y -= 1;
    }

    private void lines(String text, PDFont font, float size, Color color, float indent, float after) throws IOException {
        for (String paragraph : safe(text, font).split("\n")) {
            for (String line : wrap(paragraph, font, size, width - indent)) {
                ensureSpace(size * 1.4f);
                y -= size * 1.35f;
                text(line, font, size, color, MARGIN + indent);
            }
        }
        y -= after;
    }

    private void text(String text, PDFont font, float size, Color color, float x) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * size <= maxWidth || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
            } else {
                result.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        result.add(line.toString());
        return result;
    }

    private void ensureSpace(float needed) throws IOException {
        if (y - needed < MARGIN) {
            content.close();
            newPage();
        }
    }

    private void newPage() throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        content = new PDPageContentStream(document, page);
        width = page.getMediaBox().getWidth() - 2 * MARGIN;
        y = page.getMediaBox().getHeight() - MARGIN;
    }

    /** Sostituisce i caratteri che il font standard non può codificare (emoji, alfabeti non latini, ...). */
    static String safe(String text, PDFont font) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\t', ' ').replace("\r", "")
                .replace('→', '-').replace('–', '-').replace('—', '-');
        StringBuilder out = new StringBuilder(normalized.length());
        normalized.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            if (cp == '\n') {
                out.append('\n');
                return;
            }
            try {
                font.encode(ch);
                out.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                out.append('?');
            }
        });
        return out.toString();
    }
}

package it.cvmanager.documents.export;

import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** Impagina gli stessi blocchi del PDF in un DOCX modificabile con Word. */
final class DocxRenderer {

    private static final String FONT = "Calibri";
    private static final String TEXT = "1C1F24";
    private static final String MUTED = "5D6573";
    private static final String ACCENT = "2F6FDF";

    private DocxRenderer() {
    }

    static byte[] render(List<Block> blocks, String title) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.getProperties().getCoreProperties().setTitle(title);
            doc.getProperties().getCoreProperties().setCreator("CV Manager");
            for (Block block : blocks) {
                draw(doc, block);
            }
            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Generazione del DOCX non riuscita", e);
        }
    }

    private static void draw(XWPFDocument doc, Block block) {
        switch (block) {
            case Block.Title t -> run(paragraph(doc, 0, 80), t.text(), 20, true, false, TEXT);
            case Block.Subtitle s -> run(paragraph(doc, 0, 40), s.text(), 12, false, false, ACCENT);
            case Block.Note n -> run(paragraph(doc, 0, 40), n.text(), 9.5, false, true, MUTED);
            case Block.Paragraph p -> run(paragraph(doc, 0, 80), p.text(), 10.5, p.bold(), false, TEXT);
            case Block.Bullet b -> {
                XWPFParagraph paragraph = paragraph(doc, 0, 20);
                paragraph.setIndentationLeft(284);
                paragraph.setIndentationHanging(170);
                run(paragraph, "•\t" + b.text(), 10.5, false, false, TEXT);
            }
            case Block.Space s -> paragraph(doc, 0, 0).setSpacingAfter(Math.round(s.points() * 10));
            case Block.Section s -> {
                XWPFParagraph paragraph = paragraph(doc, 240, 80);
                paragraph.setBorderBottom(Borders.SINGLE);
                run(paragraph, s.text().toUpperCase(), 11, true, false, ACCENT);
            }
            case Block.Entry e -> {
                XWPFParagraph paragraph = paragraph(doc, 60, 20);
                run(paragraph, e.text(), 10.5, true, false, TEXT);
                if (e.right() != null && !e.right().isBlank()) {
                    run(paragraph, "  |  " + e.right(), 10.5, false, false, MUTED);
                }
            }
        }
    }

    private static XWPFParagraph paragraph(XWPFDocument doc, int before, int after) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingBefore(before);
        paragraph.setSpacingAfter(after);
        return paragraph;
    }

    private static void run(XWPFParagraph paragraph, String text, double size, boolean bold, boolean italic, String color) {
        String[] lines = (text == null ? "" : text).split("\n");
        XWPFRun run = paragraph.createRun();
        run.setFontFamily(FONT);
        run.setFontSize(size);
        run.setBold(bold);
        run.setItalic(italic);
        run.setColor(color);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                run.addBreak();
            }
            run.setText(lines[i], i);
        }
    }
}

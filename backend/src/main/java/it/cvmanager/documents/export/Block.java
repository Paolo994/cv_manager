package it.cvmanager.documents.export;

/**
 * Elemento di un documento impaginato. CV e lettere vengono prima trasformati in una lista di blocchi,
 * poi PDF e DOCX disegnano gli stessi blocchi: i due formati restano sempre allineati.
 */
public sealed interface Block {

    /** Nome in testa al documento. */
    record Title(String text) implements Block {
    }

    /** Riga secondaria sotto il titolo (ruolo, contatti). */
    record Subtitle(String text) implements Block {
    }

    /** Titolo di sezione con una riga sotto. */
    record Section(String text) implements Block {
    }

    /** Riga in grassetto con un testo secondario allineato a destra (es. ruolo e periodo). */
    record Entry(String text, String right) implements Block {
    }

    /** Testo secondario, in corsivo. */
    record Note(String text) implements Block {
    }

    record Paragraph(String text, boolean bold) implements Block {
    }

    record Bullet(String text) implements Block {
    }

    record Space(float points) implements Block {
    }
}

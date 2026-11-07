package it.cvmanager.agents;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** I prompt di sistema stanno in resources/prompts/*.md, così si modificano senza toccare il codice. */
final class Prompts {

    private Prompts() {
    }

    static String load(String name) {
        try {
            return new ClassPathResource("prompts/" + name + ".md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Prompt mancante: " + name, e);
        }
    }

    /** Racchiude un documento esterno tra tag: il modello lo tratta come dato, non come istruzioni. */
    static String document(String tag, String content) {
        // impedisce che il documento chiuda il tag e inserisca testo "fuori" dal documento
        String safe = content.replace("</" + tag + ">", "</ " + tag + ">");
        return "<" + tag + ">\n" + safe + "\n</" + tag + ">";
    }
}

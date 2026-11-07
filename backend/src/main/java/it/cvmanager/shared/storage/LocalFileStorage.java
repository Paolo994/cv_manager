package it.cvmanager.shared.storage;

import it.cvmanager.config.AppProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * File su disco sotto app.storage.local-dir. Le chiavi sono generate dal server
 * (prefisso/uuid.estensione): il nome originale del file non entra mai nel percorso.
 */
@Component
class LocalFileStorage implements FileStorage {

    private static final Pattern SAFE_SEGMENT = Pattern.compile("[a-zA-Z0-9-]{1,64}");

    private final Path root;

    LocalFileStorage(AppProperties properties) {
        this.root = Path.of(properties.storage().localDir()).toAbsolutePath().normalize();
    }

    @Override
    public String store(String prefix, String extension, byte[] content) {
        if (!SAFE_SEGMENT.matcher(prefix).matches() || !SAFE_SEGMENT.matcher(extension).matches()) {
            throw new IllegalArgumentException("Prefisso o estensione non validi");
        }
        String key = prefix + "/" + UUID.randomUUID() + "." + extension;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile salvare il file", e);
        }
        return key;
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile leggere il file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile eliminare il file", e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Chiave non valida");
        }
        return path;
    }
}

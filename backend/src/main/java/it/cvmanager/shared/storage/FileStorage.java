package it.cvmanager.shared.storage;

/** Archivio dei file caricati. In locale su disco, in produzione sostituibile con un object storage. */
public interface FileStorage {

    /** Salva il contenuto e restituisce la chiave con cui ritrovarlo. */
    String store(String prefix, String extension, byte[] content);

    byte[] read(String key);

    void delete(String key);
}

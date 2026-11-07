package it.cvmanager.shared.web;

import org.springframework.http.HttpStatus;

/**
 * Risorsa inesistente oppure di un altro utente: in entrambi i casi 404,
 * così non si rivela l'esistenza di dati altrui.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String resource) {
        super(HttpStatus.NOT_FOUND, "Non trovato", resource + " non trovato");
    }
}

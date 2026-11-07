package it.cvmanager.auth;

import it.cvmanager.shared.web.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyUsedException extends ApiException {

    public EmailAlreadyUsedException() {
        super(HttpStatus.CONFLICT, "Email già registrata", "Esiste già un account con questa email");
    }
}

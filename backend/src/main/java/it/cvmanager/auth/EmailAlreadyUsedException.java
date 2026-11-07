package it.cvmanager.auth;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("Esiste già un account con questa email");
    }
}

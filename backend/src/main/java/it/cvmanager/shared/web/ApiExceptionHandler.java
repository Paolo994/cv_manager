package it.cvmanager.shared.web;

import it.cvmanager.auth.EmailAlreadyUsedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errori applicativi restituiti come RFC 9457 Problem Details. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsedException.class)
    ProblemDetail emailAlreadyUsed(EmailAlreadyUsedException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Email già registrata");
        return problem;
    }
}

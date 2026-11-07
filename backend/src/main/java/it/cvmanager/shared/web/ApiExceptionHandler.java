package it.cvmanager.shared.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errori applicativi restituiti come RFC 9457 Problem Details. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail apiException(ApiException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(e.status(), e.getMessage());
        problem.setTitle(e.title());
        return problem;
    }
}

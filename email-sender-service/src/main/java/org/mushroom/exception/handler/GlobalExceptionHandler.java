package org.mushroom.exception.handler;

import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailEventValidationException.class)
    public ProblemDetail handleEmailEventValidationException(EmailEventValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid email event");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(OutboxEmailProcessingException.class)
    public ProblemDetail handleOutboxEmailProcessingException(OutboxEmailProcessingException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Email processing failed");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(OutboxClaimException.class)
    public ProblemDetail handleOutboxClaimException(OutboxClaimException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Outbox batch claim failed");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal server error");
        problem.setDetail("Unexpected error occurred");
        return problem;
    }
}
package org.mushroom.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.mushroom.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import javax.naming.AuthenticationException;
import java.nio.file.AccessDeniedException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;


@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final Clock clock;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        String message = "Validation failed: " + String.join(", ", errors);

        return build(message, req, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                              HttpServletRequest req) {

        return build(ex.getReason(), req, (HttpStatus) ex.getStatusCode());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex,
                                                        HttpServletRequest req) {

        return build(ex.getMessage(), req, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex,
                                                        HttpServletRequest req) {
        String message = "User not found" + ex.getMessage();
        return build(message, req, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                      HttpServletRequest req) {

        String message = "Malformed JSON request: " + ex.getMostSpecificCause().getMessage();
        return build(message, req, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest req) {

        String message = "HTTP method " + ex.getMethod() + " is not supported for this endpoint";
        return build(message, req, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest req) {

        return build("Access denied: " + ex.getMessage(), req, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest req) {

        return build("Authentication failed: " + ex.getMessage(), req, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex,
                                                       HttpServletRequest req) {

        String message = "Internal server error: " + ex.getMessage();
        return build(message, req, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenActionException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse()
                .message(ex.getMessage())
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse()
                .message("Authentication failed: Bad credentials")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex,
                                                                HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse()
                .message("Authentication failed: User not found")
                .timestamp(OffsetDateTime.now(clock))
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AnalyticsBuildException.class)
    public ResponseEntity<ErrorResponse> handleAnalyticsBuild(
            AnalyticsBuildException ex,
            HttpServletRequest request
    ) {
        ErrorResponse body = new ErrorResponse()
                .message("Daily analytics is temporarily unavailable")
                .timestamp(OffsetDateTime.now())
                .path(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ErrorResponse> build(String message,
                                                HttpServletRequest req,
                                                HttpStatus status) {

        ErrorResponse body = new ErrorResponse()
                .message(message)
                .timestamp(OffsetDateTime.now(clock))
                .path(req.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}


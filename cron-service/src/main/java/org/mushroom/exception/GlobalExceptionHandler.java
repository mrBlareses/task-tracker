package org.mushroom.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.model.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErrorResponse> handleFeignException(
            FeignException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = mapFeignStatus(ex.status());
        String message = buildFeignMessage(ex);

        log.error("Feign exception handled, status={}, path={}, message={}",
                ex.status(), request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(status, message, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception, path={}, message={}",
                request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private HttpStatus mapFeignStatus(int status) {
        if (status == 400) return HttpStatus.BAD_REQUEST;
        if (status == 401) return HttpStatus.UNAUTHORIZED;
        if (status == 403) return HttpStatus.FORBIDDEN;
        if (status == 404) return HttpStatus.NOT_FOUND;
        if (status == 502) return HttpStatus.BAD_GATEWAY;
        if (status == 503) return HttpStatus.SERVICE_UNAVAILABLE;
        if (status == 504) return HttpStatus.GATEWAY_TIMEOUT;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String buildFeignMessage(FeignException ex) {
        return switch (ex.status()) {
            case 401 -> "Backend authentication failed";
            case 502, 503, 504 -> "Backend service is temporarily unavailable";
            default -> "Backend request failed";
        };
    }
}

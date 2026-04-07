package org.mushroom.handler;

import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mushroom.exception.GlobalExceptionHandler;
import org.mushroom.model.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerUnitTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleIllegalArgument — возвращает 400")
    void handleIllegalArgument_ShouldReturnBadRequest() {
        MockHttpServletRequest request = request();

        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad request"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("bad request");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/internal/cron/daily-analytics/run");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleIllegalState — возвращает 500")
    void handleIllegalState_ShouldReturnInternalServerError() {
        MockHttpServletRequest request = request();

        ResponseEntity<ApiErrorResponse> response =
                handler.handleIllegalState(new IllegalStateException("internal error"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("internal error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/internal/cron/daily-analytics/run");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("handleFeignException — мапит 400/401/403/404")
    void handleFeignException_ShouldMapClientStatuses() {
        assertFeignMapping(400, HttpStatus.BAD_REQUEST, "Backend request failed");
        assertFeignMapping(401, HttpStatus.UNAUTHORIZED, "Backend authentication failed");
        assertFeignMapping(403, HttpStatus.FORBIDDEN, "Backend request failed");
        assertFeignMapping(404, HttpStatus.NOT_FOUND, "Backend request failed");
    }

    @Test
    @DisplayName("handleFeignException — мапит 502/503/504")
    void handleFeignException_ShouldMapGatewayStatuses() {
        assertFeignMapping(502, HttpStatus.BAD_GATEWAY, "Backend service is temporarily unavailable");
        assertFeignMapping(503, HttpStatus.SERVICE_UNAVAILABLE, "Backend service is temporarily unavailable");
        assertFeignMapping(504, HttpStatus.GATEWAY_TIMEOUT, "Backend service is temporarily unavailable");
    }

    @Test
    @DisplayName("handleFeignException — неизвестный статус мапится в 500")
    void handleFeignException_ShouldFallbackTo500() {
        assertFeignMapping(418, HttpStatus.INTERNAL_SERVER_ERROR, "Backend request failed");
    }

    @Test
    @DisplayName("handleGenericException — возвращает 500 и стандартное сообщение")
    void handleGenericException_ShouldReturn500() {
        MockHttpServletRequest request = request();

        ResponseEntity<ApiErrorResponse> response =
                handler.handleGenericException(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/internal/cron/daily-analytics/run");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    private void assertFeignMapping(int feignStatus, HttpStatus expectedStatus, String expectedMessage) {
        FeignException ex = mock(FeignException.class);
        when(ex.status()).thenReturn(feignStatus);
        when(ex.getMessage()).thenReturn("feign error " + feignStatus);

        ResponseEntity<ApiErrorResponse> response = handler.handleFeignException(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expectedStatus.value());
        assertThat(response.getBody().getError()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(response.getBody().getMessage()).isEqualTo(expectedMessage);
        assertThat(response.getBody().getPath()).isEqualTo("/api/v1/internal/cron/daily-analytics/run");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/internal/cron/daily-analytics/run");
        return request;
    }
}
package org.mushroom.service;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.config.BackendRetryProperties;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackendTriggerServiceUnitTest {

    @Mock
    private BackendGateway backendGateway;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private BackendRetryProperties backendRetryProperties;

    @InjectMocks
    private BackendTriggerService backendTriggerService;

    @BeforeEach
    void setUp() {
        when(backendRetryProperties.getMaxAttempts()).thenReturn(3);
    }

    @Test
    @DisplayName("triggerDailyAnalytics — вызывает backend с token")
    void triggerDailyAnalytics_ShouldCallBackend() {
        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        backendTriggerService.triggerDailyAnalytics();

        verify(backendGateway).triggerDailyAnalytics("token-1");
        verify(accessTokenService, never()).evictToken();
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при повторном 401 после refresh не делает второй refresh и пробрасывает исключение")
    void triggerDailyAnalytics_ShouldThrow_WhenSecondAttemptAlsoReturns401() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        FeignException unauthorized = feignException(401);

        when(accessTokenService.getAccessToken()).thenReturn("old-token", "new-token");

        doThrow(unauthorized)
                .doThrow(unauthorized)
                .when(backendGateway)
                .triggerDailyAnalytics(anyString());

        assertThrows(FeignException.class, () -> backendTriggerService.triggerDailyAnalytics());

        verify(accessTokenService, times(1)).evictToken();
        verify(backendGateway).triggerDailyAnalytics("old-token");
        verify(backendGateway).triggerDailyAnalytics("new-token");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при 401 сбрасывает токен и повторяет вызов")
    void triggerDailyAnalytics_ShouldRetryOn401() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        FeignException unauthorized = feignException(401);
        when(accessTokenService.getAccessToken()).thenReturn("old-token", "new-token");

        doThrow(unauthorized)
                .doNothing()
                .when(backendGateway)
                .triggerDailyAnalytics(anyString());

        backendTriggerService.triggerDailyAnalytics();

        verify(accessTokenService).evictToken();
        verify(backendGateway).triggerDailyAnalytics("old-token");
        verify(backendGateway).triggerDailyAnalytics("new-token");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при 401 и повторной ошибке пробрасывает исключение")
    void triggerDailyAnalytics_ShouldThrow_When401AndRetryFails() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        FeignException unauthorized = feignException(401);
        FeignException internalError = feignException(500);

        when(accessTokenService.getAccessToken()).thenReturn("old-token", "new-token");

        doThrow(unauthorized)
                .doThrow(internalError)
                .when(backendGateway)
                .triggerDailyAnalytics(anyString());

        assertThrows(FeignException.class, () -> backendTriggerService.triggerDailyAnalytics());

        verify(accessTokenService, times(1)).evictToken();
        verify(backendGateway).triggerDailyAnalytics("old-token");
        verify(backendGateway).triggerDailyAnalytics("new-token");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при 502 делает retry и потом успешно завершает")
    void triggerDailyAnalytics_ShouldRetryOn502() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        FeignException badGateway = feignException(502);
        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(badGateway)
                .doNothing()
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        backendTriggerService.triggerDailyAnalytics();

        verify(backendGateway, times(2)).triggerDailyAnalytics("token-1");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при 503 делает retry и потом успешно завершает")
    void triggerDailyAnalytics_ShouldRetryOn503() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        FeignException unavailable = feignException(503);
        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(unavailable)
                .doNothing()
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        backendTriggerService.triggerDailyAnalytics();

        verify(backendGateway, times(2)).triggerDailyAnalytics("token-1");
        verify(accessTokenService, never()).evictToken();
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при 504 делает retry и потом успешно завершает")
    void triggerDailyAnalytics_ShouldRetryOn504() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        FeignException gatewayTimeout = feignException(504);
        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(gatewayTimeout)
                .doNothing()
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        backendTriggerService.triggerDailyAnalytics();

        verify(backendGateway, times(2)).triggerDailyAnalytics("token-1");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при RetryableException делает retry")
    void triggerDailyAnalytics_ShouldRetryOnRetryableException() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);

        RetryableException retryableException = new RetryableException(
                503,
                "temporary network issue",
                Request.HttpMethod.POST,
                null,
                (Long) null,
                request()
        );

        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(retryableException)
                .doNothing()
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        backendTriggerService.triggerDailyAnalytics();

        verify(backendGateway, times(2)).triggerDailyAnalytics("token-1");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при RetryableException после исчерпания retry пробрасывает исключение")
    void triggerDailyAnalytics_ShouldThrowAfterMaxAttempts_OnRetryableException() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);
        when(backendRetryProperties.getSecondBackoff()).thenReturn(Duration.ZERO);

        RetryableException retryableException = new RetryableException(
                503,
                "temporary network issue",
                Request.HttpMethod.POST,
                null,
                (Long) null,
                request()
        );

        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(retryableException)
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        assertThrows(RetryableException.class, () -> backendTriggerService.triggerDailyAnalytics());

        verify(backendGateway, times(3)).triggerDailyAnalytics("token-1");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — после исчерпания retry пробрасывает исключение")
    void triggerDailyAnalytics_ShouldThrowAfterMaxAttempts() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);
        when(backendRetryProperties.getSecondBackoff()).thenReturn(Duration.ZERO);

        FeignException unavailable = feignException(503);
        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(unavailable)
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        assertThrows(FeignException.class, () -> backendTriggerService.triggerDailyAnalytics());

        verify(backendGateway, times(3)).triggerDailyAnalytics("token-1");
    }

    @Test
    @DisplayName("triggerDailyAnalytics — использует thirdBackoff начиная с третьей попытки")
    void triggerDailyAnalytics_ShouldUseThirdBackoff_FromThirdAttempt() {
        when(backendRetryProperties.getMaxAttempts()).thenReturn(4);
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ZERO);
        when(backendRetryProperties.getSecondBackoff()).thenReturn(Duration.ZERO);
        when(backendRetryProperties.getThirdBackoff()).thenReturn(Duration.ZERO);

        FeignException unavailable = feignException(503);
        when(accessTokenService.getAccessToken()).thenReturn("token-1");

        doThrow(unavailable)
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        assertThrows(FeignException.class, () -> backendTriggerService.triggerDailyAnalytics());

        verify(backendGateway, times(4)).triggerDailyAnalytics("token-1");
        verify(backendRetryProperties, atLeastOnce()).getThirdBackoff();
    }

    @Test
    @DisplayName("triggerDailyAnalytics — при 500 не делает retry")
    void triggerDailyAnalytics_ShouldNotRetryOn500() {
        FeignException serverError = feignException(500);

        when(accessTokenService.getAccessToken()).thenReturn("token-1");
        doThrow(serverError)
                .when(backendGateway)
                .triggerDailyAnalytics("token-1");

        assertThrows(FeignException.class, () -> backendTriggerService.triggerDailyAnalytics());

        verify(backendGateway, times(1)).triggerDailyAnalytics("token-1");
        verify(accessTokenService, never()).evictToken();
    }

    @Test
    @DisplayName("triggerDailyAnalytics — если sleep interrupted, бросает IllegalStateException")
    void triggerDailyAnalytics_ShouldThrowIllegalState_WhenSleepInterrupted() {
        when(backendRetryProperties.getFirstBackoff()).thenReturn(Duration.ofMillis(1));

        FeignException unavailable = feignException(503);
        when(accessTokenService.getAccessToken()).thenReturn("token-1");
        doThrow(unavailable).when(backendGateway).triggerDailyAnalytics("token-1");

        Thread.currentThread().interrupt();
        try {
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> backendTriggerService.triggerDailyAnalytics()
            );

            assertThat(ex.getMessage()).isEqualTo("Retry sleep interrupted");
        } finally {
            Thread.interrupted();
        }
    }

    private FeignException feignException(int status) {
        return FeignException.errorStatus(
                "triggerDailyAnalytics",
                feign.Response.builder()
                        .status(status)
                        .reason("error")
                        .request(request())
                        .headers(Collections.emptyMap())
                        .body(new byte[0])
                        .build()
        );
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.POST,
                "http://backend-service:8081/api/v1/service/daily-analytics/run",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
    }
}
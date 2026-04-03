package org.mushroom.service;

import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.config.BackendRetryProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendTriggerService {

    private final BackendGateway backendGateway;
    private final AccessTokenService accessTokenService;
    private final BackendRetryProperties backendRetryProperties;

    public void triggerDailyAnalytics() {
        int attempt = 1;
        int maxAttempts = backendRetryProperties.getMaxAttempts();

        while (true) {
            try {
                backendGateway.triggerDailyAnalytics(accessTokenService.getAccessToken());
                log.info("Backend analytics endpoint accepted request successfully, attempt={}", attempt);
                return;

            } catch (RetryableException ex) {
                if (attempt < maxAttempts) {
                    long delayMs = resolveBackoff(attempt);
                    log.warn(
                            "Retryable network error during backend trigger, attempt={}, maxAttempts={}, retryInMs={}, message={}",
                            attempt, maxAttempts, delayMs, ex.getMessage()
                    );
                    sleep(delayMs);
                    attempt++;
                    continue;
                }

                log.error(
                        "Backend trigger failed after retryable exception, finalAttempt={}, maxAttempts={}, message={}",
                        attempt, maxAttempts, ex.getMessage(), ex
                );
                throw ex;

            } catch (FeignException ex) {
                if (ex.status() == 401 && attempt == 1) {
                    log.warn("Received 401 from backend, evicting token and retrying once, attempt={}", attempt);
                    accessTokenService.evictToken();
                    sleep(resolveBackoff(attempt));
                    attempt++;
                    continue;
                }

                if (isTransientStatus(ex.status()) && attempt < maxAttempts) {
                    long delayMs = resolveBackoff(attempt);
                    log.warn(
                            "Transient backend error, status={}, attempt={}, maxAttempts={}, retryInMs={}",
                            ex.status(), attempt, maxAttempts, delayMs
                    );
                    sleep(delayMs);
                    attempt++;
                    continue;
                }

                log.error(
                        "Backend trigger failed, status={}, finalAttempt={}, maxAttempts={}, message={}",
                        ex.status(), attempt, maxAttempts, ex.getMessage(), ex
                );
                throw ex;
            }
        }
    }

    private boolean isTransientStatus(int status) {
        return status == 502 || status == 503 || status == 504;
    }

    private long resolveBackoff(int attempt) {
        return switch (attempt) {
            case 1 -> backendRetryProperties.getFirstBackoff().toMillis();
            case 2 -> backendRetryProperties.getSecondBackoff().toMillis();
            default -> backendRetryProperties.getThirdBackoff().toMillis();
        };
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry sleep interrupted", e);
        }
    }
}
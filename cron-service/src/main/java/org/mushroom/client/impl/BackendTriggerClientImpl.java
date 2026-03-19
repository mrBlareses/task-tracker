package org.mushroom.client.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.client.BackendTriggerClient;
import org.mushroom.config.SchedulerProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackendTriggerClientImpl implements BackendTriggerClient {

    private final SchedulerProperties schedulerProperties;

    @Override
    public void triggerDailyAnalytics() {
        String url = schedulerProperties.getBackendBaseUrl() + schedulerProperties.getTriggerPath();

        log.info("Calling backend analytics endpoint: {}", url);

        WebClient.create()
                .post()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .map(body -> new IllegalStateException(
                                        "Backend returned error status " + response.statusCode() + ", body=" + body
                                ))
                )
                .toBodilessEntity()
                .block();

        log.info("Backend analytics endpoint accepted request successfully");
    }
}

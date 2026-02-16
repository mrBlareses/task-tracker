package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.kafka.publisher.EventPublisher;
import org.mushroom.reports.UserRegisteredReport;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationAnalyticsService {

    private final EventPublisher eventPublisher;

    @Async("analyticsExecutor")
    public void publishRegistrationEvent(UserRegisteredReport report) {
        try {
            eventPublisher.publishUserRegistrationEvent(report);
            log.info("published registration event for user {}", report.userId());
        } catch (Exception e) {
            log.error("Failed to publish kafka event for user {}", report.userId(), e);
        }
    }
}
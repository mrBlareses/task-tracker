package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.kafka.EventPublisher;
import org.mushroom.reports.UserDailyReportProjection;
import org.mushroom.repository.TaskRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAnalyticsService {
    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;

    @Async("analyticsExecutor")
    public void collectAndSendDailyAnalytics() {
        for (UserDailyReportProjection report : taskRepository.getDailyReports()) {
            eventPublisher.publishDailyAnalyticsEvent(report);
        }
    }
}
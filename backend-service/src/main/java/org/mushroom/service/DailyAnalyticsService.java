package org.mushroom.service;


import lombok.RequiredArgsConstructor;
import org.mushroom.exception.AnalyticsBuildException;
import org.mushroom.repository.TaskRepository;
import org.mushroom.service.model.DailyReport;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.mushroom.service.model.DailyAnalyticsResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;


@Service
@RequiredArgsConstructor
public class DailyAnalyticsService {
    private final TaskRepository taskRepository;

    @Async("analyticsExecutor")
    public CompletableFuture<DailyAnalyticsResponse> getDailyAnalytics() {
        final List<DailyReport> summaries;
        try {
            summaries = taskRepository.getDailyReports().stream()
                    .map(r -> new DailyReport(
                            r.getUserId(),
                            r.getEmail(),
                            r.getPendingCount(),
                            r.getCompletedCount()
                    )).toList();
            return CompletableFuture.completedFuture(new DailyAnalyticsResponse(summaries));
        } catch (DataAccessException e) {
            return CompletableFuture.failedFuture(new AnalyticsBuildException("Failed to build daily analytics", e));
        }
    }
}
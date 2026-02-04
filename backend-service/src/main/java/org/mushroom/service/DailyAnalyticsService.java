package org.mushroom.service;


import lombok.RequiredArgsConstructor;
import org.mushroom.exception.AnalyticsBuildException;
import org.mushroom.repository.TaskRepository;
import org.mushroom.service.model.DailyReport;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.mushroom.service.model.DailyAnalyticsResponse;

import java.util.*;


@Service
@RequiredArgsConstructor
public class DailyAnalyticsService {
    private final TaskRepository taskRepository;

    public DailyAnalyticsResponse getDailyAnalytics() {
        final List<DailyReport> summaries;
        try {
            summaries = taskRepository.getDailyReports().stream()
                    .map(r -> new DailyReport(
                            r.getUserId(),
                            r.getEmail(),
                            r.getPendingCount(),
                            r.getCompletedCount()
                    ))
                    .sorted(Comparator.comparing(DailyReport::getUserId))
                    .toList();
        } catch (DataAccessException e) {
            throw new AnalyticsBuildException("Failed to build daily analytics", e);
        }
        return new DailyAnalyticsResponse(summaries);
    }
}


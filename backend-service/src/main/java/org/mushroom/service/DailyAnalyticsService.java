package org.mushroom.service;


import lombok.RequiredArgsConstructor;
import org.mushroom.exception.AnalyticsBuildException;
import org.mushroom.repository.TaskRepository;
import org.mushroom.service.model.UserDailyReport;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.mushroom.service.model.DailyAnalyticsResponse;


import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;


@Service
@RequiredArgsConstructor
public class DailyAnalyticsService {
    private final TaskRepository taskRepository;
    private final Clock clock;

    public DailyAnalyticsResponse getDailyAnalytics() {
        OffsetDateTime now = OffsetDateTime.now(clock);

        ZoneOffset offset = now.getOffset();
        OffsetDateTime to = now.toLocalDate()
                .atStartOfDay()
                .atOffset(offset);
        OffsetDateTime from = to.minusDays(1);

        final List<UserDailyReport> summaries;
        try {
            summaries = taskRepository.getDailyReports().stream()
                    .map(r -> new UserDailyReport(
                            r.getUserId(),
                            r.getEmail(),
                            r.getPendingCount(),
                            r.getCompletedCount()
                    ))
                    .sorted(Comparator.comparing(UserDailyReport::getUserId))
                    .toList();
        } catch (DataAccessException e) {
            throw new AnalyticsBuildException("Failed to build daily analytics", e);
        }
        return new DailyAnalyticsResponse(from, to, summaries);
    }
}


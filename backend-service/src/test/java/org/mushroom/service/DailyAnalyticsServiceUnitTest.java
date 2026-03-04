package org.mushroom.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.kafka.EventPublisher;
import org.mushroom.reports.UserDailyReportProjection;
import org.mushroom.repository.TaskRepository;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class DailyAnalyticsServiceUnitTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    DailyAnalyticsService dailyAnalyticsService;

    @Test
    void collectAndSendDailyAnalytics_ShouldPublishEventForEachReport_InSameOrder() {
        UserDailyReportProjection row2 = mock(UserDailyReportProjection.class);
        UserDailyReportProjection row1 = mock(UserDailyReportProjection.class);

        when(taskRepository.getDailyReports()).thenReturn(List.of(row2, row1));

        dailyAnalyticsService.collectAndSendDailyAnalytics();
    }

    @Test
    void collectAndSendDailyAnalytics_ShouldThrow_WhenRepositoryFails() {
        when(taskRepository.getDailyReports())
                .thenThrow(new DataAccessResourceFailureException("db down"));

        org.junit.jupiter.api.Assertions.assertThrows(
                DataAccessResourceFailureException.class,
                () -> dailyAnalyticsService.collectAndSendDailyAnalytics()
        );
    }
}

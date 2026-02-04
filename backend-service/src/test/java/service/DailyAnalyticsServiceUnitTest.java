package service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.exception.AnalyticsBuildException;
import org.mushroom.repository.DailyReportRow;
import org.mushroom.repository.TaskRepository;
import org.mushroom.service.DailyAnalyticsService;
import org.mushroom.service.model.DailyAnalyticsResponse;
import org.mushroom.service.model.UserDailyReport;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DailyAnalyticsServiceUnitTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private DailyAnalyticsService dailyAnalyticsService;

    @Test
    void getDailyAnalytics_ShouldReturnResponse_WithSortedSummaries() {
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T08:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));

        UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        DailyReportRow row2 = row(id2, "b@test.com", 5L, 1L);
        DailyReportRow row1 = row(id1, "a@test.com", 2L, 3L);

        when(taskRepository.getDailyReports(any(), any()))
                .thenReturn(List.of(row2, row1));

        DailyAnalyticsResponse response = dailyAnalyticsService.getDailyAnalytics();

        assertThat(response.getFrom()).isNotNull();
        assertThat(response.getTo()).isNotNull();

        List<UserDailyReport> summaries = response.getSummaries();
        assertThat(summaries).hasSize(2);

        assertThat(summaries.get(0).getUserId()).isEqualTo(id1);
        assertThat(summaries.get(1).getUserId()).isEqualTo(id2);

        assertThat(summaries.get(0).getEmail()).isEqualTo("a@test.com");
        assertThat(summaries.get(0).getPendingCount()).isEqualTo(2L);
        assertThat(summaries.get(0).getCompletedCount()).isEqualTo(3L);
    }

    @Test
    void getDailyAnalytics_ShouldThrowAnalyticsBuildException_WhenRepositoryFails() {
        when(clock.instant()).thenReturn(Instant.parse("2026-02-02T08:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Moscow"));

        when(taskRepository.getDailyReports(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        AnalyticsBuildException ex = assertThrows(
                AnalyticsBuildException.class,
                () -> dailyAnalyticsService.getDailyAnalytics()
        );

        assertThat(ex.getMessage()).isEqualTo("Failed to build daily analytics");
        assertThat(ex.getCause()).isInstanceOf(DataAccessResourceFailureException.class);
    }

    private static DailyReportRow row(UUID userId, String email, Long pending, Long completed) {
        return new DailyReportRow() {
            @Override public UUID getUserId() { return userId; }
            @Override public String getEmail() { return email; }
            @Override public long getPendingCount() { return pending; }
            @Override public long getCompletedCount() { return completed; }
        };
    }
}

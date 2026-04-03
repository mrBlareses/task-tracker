package org.mushroom.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.model.JobExecutionResponse;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.mushroom.service.JobExecutionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyAnalyticsControllerUnitTest {

    @Mock
    private DailyAnalyticsTriggerService dailyAnalyticsTriggerService;

    @Mock
    private JobExecutionService jobExecutionService;

    @InjectMocks
    private DailyAnalyticsController dailyAnalyticsController;

    @Test
    @DisplayName("runDailyAnalyticsManually — вызывает trigger с MANUAL и возвращает 202")
    void runDailyAnalyticsManually_ShouldReturnAccepted() {
        ResponseEntity<Void> response = dailyAnalyticsController.runDailyAnalyticsManually();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(dailyAnalyticsTriggerService).trigger(TriggerType.MANUAL);
    }

    @Test
    @DisplayName("getRecentExecutions — возвращает список execution history")
    void getRecentExecutions_ShouldReturnExecutionList() {
        List<JobExecutionResponse> responses = List.of(
                JobExecutionResponse.builder()
                        .id(UUID.randomUUID())
                        .jobName("DAILY_ANALYTICS")
                        .status(JobExecutionStatus.SUCCESS)
                        .triggerType(TriggerType.CRON)
                        .startedAt(OffsetDateTime.now().minusMinutes(1))
                        .finishedAt(OffsetDateTime.now())
                        .build()
        );

        when(jobExecutionService.getRecentExecutions("DAILY_ANALYTICS")).thenReturn(responses);

        ResponseEntity<List<JobExecutionResponse>> response = dailyAnalyticsController.getRecentExecutions();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responses);
    }
}
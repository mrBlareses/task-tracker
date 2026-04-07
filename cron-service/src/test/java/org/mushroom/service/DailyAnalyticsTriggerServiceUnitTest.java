package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.config.SchedulerProperties;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyAnalyticsTriggerServiceUnitTest {

    @Mock
    private BackendTriggerService backendTriggerService;

    @Mock
    private JobExecutionService jobExecutionService;

    @Mock
    private SchedulerProperties schedulerProperties;

    @InjectMocks
    private DailyAnalyticsTriggerService dailyAnalyticsTriggerService;

    @Test
    @DisplayName("trigger — happy path: создаёт RUNNING, вызывает backend и помечает SUCCESS")
    void trigger_ShouldCompleteSuccessfully() {
        UUID executionId = UUID.randomUUID();

        JobExecutionEntity execution = JobExecutionEntity.builder()
                .id(executionId)
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.RUNNING)
                .triggerType(TriggerType.CRON)
                .startedAt(OffsetDateTime.now())
                .build();

        when(schedulerProperties.getStaleThreshold()).thenReturn(Duration.ofMinutes(15));
        when(jobExecutionService.deleteStaleRunningExecutions(eq("DAILY_ANALYTICS"), any(OffsetDateTime.class)))
                .thenReturn(0L);
        when(jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.CRON))
                .thenReturn(Optional.of(execution));

        dailyAnalyticsTriggerService.trigger(TriggerType.CRON);

        verify(backendTriggerService).triggerDailyAnalytics();
        verify(jobExecutionService).markSuccess(executionId);
        verify(jobExecutionService, never()).deleteExecution(any());
    }

    @Test
    @DisplayName("trigger — если execution уже RUNNING, backend не вызывается")
    void trigger_ShouldSkipWhenExecutionAlreadyRunning() {
        when(schedulerProperties.getStaleThreshold()).thenReturn(Duration.ofMinutes(15));
        when(jobExecutionService.deleteStaleRunningExecutions(eq("DAILY_ANALYTICS"), any(OffsetDateTime.class)))
                .thenReturn(0L);
        when(jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.MANUAL))
                .thenReturn(Optional.empty());

        dailyAnalyticsTriggerService.trigger(TriggerType.MANUAL);

        verify(backendTriggerService, never()).triggerDailyAnalytics();
        verify(jobExecutionService, never()).markSuccess(any());
        verify(jobExecutionService, never()).deleteExecution(any());
    }

    @Test
    @DisplayName("trigger — при ошибке backend удаляет RUNNING execution и пробрасывает исключение")
    void trigger_ShouldDeleteExecutionWhenBackendFails() {
        UUID executionId = UUID.randomUUID();

        JobExecutionEntity execution = JobExecutionEntity.builder()
                .id(executionId)
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.RUNNING)
                .triggerType(TriggerType.CRON)
                .startedAt(OffsetDateTime.now())
                .build();

        when(schedulerProperties.getStaleThreshold()).thenReturn(Duration.ofMinutes(15));
        when(jobExecutionService.deleteStaleRunningExecutions(eq("DAILY_ANALYTICS"), any(OffsetDateTime.class)))
                .thenReturn(1L);
        when(jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.CRON))
                .thenReturn(Optional.of(execution));

        doThrow(new IllegalStateException("backend error"))
                .when(backendTriggerService)
                .triggerDailyAnalytics();

        assertThrows(
                IllegalStateException.class,
                () -> dailyAnalyticsTriggerService.trigger(TriggerType.CRON)
        );

        verify(jobExecutionService).deleteExecution(executionId);
        verify(jobExecutionService, never()).markSuccess(any());
    }

    @Test
    @DisplayName("trigger — если markSuccess падает, execution удаляется и исключение пробрасывается")
    void trigger_ShouldDeleteExecutionWhenMarkSuccessFails() {
        UUID executionId = UUID.randomUUID();

        JobExecutionEntity execution = JobExecutionEntity.builder()
                .id(executionId)
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.RUNNING)
                .triggerType(TriggerType.MANUAL)
                .startedAt(OffsetDateTime.now())
                .build();

        when(schedulerProperties.getStaleThreshold()).thenReturn(Duration.ofMinutes(15));
        when(jobExecutionService.deleteStaleRunningExecutions(eq("DAILY_ANALYTICS"), any(OffsetDateTime.class)))
                .thenReturn(0L);
        when(jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.MANUAL))
                .thenReturn(Optional.of(execution));

        doThrow(new IllegalStateException("mark success failed"))
                .when(jobExecutionService)
                .markSuccess(executionId);

        assertThrows(
                IllegalStateException.class,
                () -> dailyAnalyticsTriggerService.trigger(TriggerType.MANUAL)
        );

        verify(backendTriggerService).triggerDailyAnalytics();
        verify(jobExecutionService).markSuccess(executionId);
        verify(jobExecutionService).deleteExecution(executionId);
    }

    @Test
    @DisplayName("trigger — если удаление stale executions падает, дальнейшие действия не выполняются")
    void trigger_ShouldFailFast_WhenDeleteStaleRunningExecutionsFails() {
        when(schedulerProperties.getStaleThreshold()).thenReturn(Duration.ofMinutes(15));
        when(jobExecutionService.deleteStaleRunningExecutions(eq("DAILY_ANALYTICS"), any(OffsetDateTime.class)))
                .thenThrow(new IllegalStateException("db error"));

        assertThrows(
                IllegalStateException.class,
                () -> dailyAnalyticsTriggerService.trigger(TriggerType.CRON)
        );

        verify(jobExecutionService, never()).tryCreateRunningExecution(anyString(), any());
        verify(backendTriggerService, never()).triggerDailyAnalytics();
        verify(jobExecutionService, never()).markSuccess(any());
        verify(jobExecutionService, never()).deleteExecution(any());
    }
}
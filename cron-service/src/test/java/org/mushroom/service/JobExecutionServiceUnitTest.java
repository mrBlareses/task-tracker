package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.model.JobExecutionResponse;
import org.mushroom.repository.JobExecutionRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceUnitTest {

    @Mock
    private JobExecutionRepository jobExecutionRepository;

    @InjectMocks
    private JobExecutionService jobExecutionService;

    @Test
    @DisplayName("tryCreateRunningExecution — создаёт RUNNING execution")
    void tryCreateRunningExecution_ShouldReturnExecution() {
        JobExecutionEntity saved = JobExecutionEntity.builder()
                .id(UUID.randomUUID())
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.RUNNING)
                .triggerType(TriggerType.CRON)
                .startedAt(OffsetDateTime.now())
                .build();

        when(jobExecutionRepository.saveAndFlush(any(JobExecutionEntity.class))).thenReturn(saved);

        Optional<JobExecutionEntity> actual =
                jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.CRON);

        assertThat(actual).isPresent();
        assertThat(actual.get().getStatus()).isEqualTo(JobExecutionStatus.RUNNING);
    }

    @Test
    @DisplayName("tryCreateRunningExecution — заполняет поля новой сущности перед сохранением")
    void tryCreateRunningExecution_ShouldPopulateEntityFields() {
        JobExecutionEntity saved = JobExecutionEntity.builder()
                .id(UUID.randomUUID())
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.RUNNING)
                .triggerType(TriggerType.MANUAL)
                .startedAt(OffsetDateTime.now())
                .build();

        when(jobExecutionRepository.saveAndFlush(any(JobExecutionEntity.class))).thenReturn(saved);

        jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.MANUAL);

        ArgumentCaptor<JobExecutionEntity> captor = ArgumentCaptor.forClass(JobExecutionEntity.class);
        verify(jobExecutionRepository).saveAndFlush(captor.capture());

        JobExecutionEntity captured = captor.getValue();
        assertThat(captured.getId()).isNotNull();
        assertThat(captured.getJobName()).isEqualTo("DAILY_ANALYTICS");
        assertThat(captured.getStatus()).isEqualTo(JobExecutionStatus.RUNNING);
        assertThat(captured.getTriggerType()).isEqualTo(TriggerType.MANUAL);
        assertThat(captured.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("tryCreateRunningExecution — возвращает empty если уже есть RUNNING execution")
    void tryCreateRunningExecution_ShouldReturnEmptyOnConflict() {
        when(jobExecutionRepository.saveAndFlush(any(JobExecutionEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Optional<JobExecutionEntity> actual =
                jobExecutionService.tryCreateRunningExecution("DAILY_ANALYTICS", TriggerType.CRON);

        assertThat(actual).isEmpty();
    }

    @Test
    @DisplayName("markSuccess — переводит execution в SUCCESS")
    void markSuccess_ShouldUpdateExecution() {
        UUID executionId = UUID.randomUUID();
        JobExecutionEntity execution = JobExecutionEntity.builder()
                .id(executionId)
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.RUNNING)
                .triggerType(TriggerType.CRON)
                .startedAt(OffsetDateTime.now())
                .build();

        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.of(execution));

        jobExecutionService.markSuccess(executionId);

        assertThat(execution.getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(execution.getFinishedAt()).isNotNull();

        verify(jobExecutionRepository).save(execution);
    }

    @Test
    @DisplayName("markSuccess — бросает IllegalStateException если execution не найден")
    void markSuccess_ShouldThrowWhenNotFound() {
        UUID executionId = UUID.randomUUID();
        when(jobExecutionRepository.findById(executionId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> jobExecutionService.markSuccess(executionId)
        );

        assertThat(ex.getMessage()).contains(executionId.toString());
    }

    @Test
    @DisplayName("deleteExecution — удаляет execution по id")
    void deleteExecution_ShouldCallRepository() {
        UUID executionId = UUID.randomUUID();

        jobExecutionService.deleteExecution(executionId);

        verify(jobExecutionRepository).deleteById(executionId);
    }

    @Test
    @DisplayName("deleteStaleRunningExecutions — удаляет stale RUNNING executions")
    void deleteStaleRunningExecutions_ShouldReturnDeletedCount() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(15);

        when(jobExecutionRepository.deleteByJobNameAndStatusAndStartedAtBefore(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                threshold
        )).thenReturn(2L);

        long deleted = jobExecutionService.deleteStaleRunningExecutions("DAILY_ANALYTICS", threshold);

        assertThat(deleted).isEqualTo(2L);
    }

    @Test
    @DisplayName("deleteStaleRunningExecutions — возвращает 0 если ничего не удалено")
    void deleteStaleRunningExecutions_ShouldReturnZeroWhenNothingDeleted() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(15);

        when(jobExecutionRepository.deleteByJobNameAndStatusAndStartedAtBefore(
                "DAILY_ANALYTICS",
                JobExecutionStatus.RUNNING,
                threshold
        )).thenReturn(0L);

        long deleted = jobExecutionService.deleteStaleRunningExecutions("DAILY_ANALYTICS", threshold);

        assertThat(deleted).isZero();
    }

    @Test
    @DisplayName("getRecentExecutions — мапит entities в response")
    void getRecentExecutions_ShouldMapResponses() {
        OffsetDateTime startedAt = OffsetDateTime.now().minusMinutes(1);
        OffsetDateTime finishedAt = OffsetDateTime.now();

        JobExecutionEntity entity = JobExecutionEntity.builder()
                .id(UUID.randomUUID())
                .jobName("DAILY_ANALYTICS")
                .status(JobExecutionStatus.SUCCESS)
                .triggerType(TriggerType.MANUAL)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .build();

        when(jobExecutionRepository.findTop20ByJobNameOrderByStartedAtDesc("DAILY_ANALYTICS"))
                .thenReturn(List.of(entity));

        List<JobExecutionResponse> actual = jobExecutionService.getRecentExecutions("DAILY_ANALYTICS");

        assertThat(actual).hasSize(1);
        assertThat(actual.get(0).getId()).isEqualTo(entity.getId());
        assertThat(actual.get(0).getJobName()).isEqualTo(entity.getJobName());
        assertThat(actual.get(0).getStatus()).isEqualTo(JobExecutionStatus.SUCCESS);
        assertThat(actual.get(0).getTriggerType()).isEqualTo(TriggerType.MANUAL);
        assertThat(actual.get(0).getStartedAt()).isEqualTo(startedAt);
        assertThat(actual.get(0).getFinishedAt()).isEqualTo(finishedAt);
    }

    @Test
    @DisplayName("getRecentExecutions — возвращает пустой список если записей нет")
    void getRecentExecutions_ShouldReturnEmptyList() {
        when(jobExecutionRepository.findTop20ByJobNameOrderByStartedAtDesc("DAILY_ANALYTICS"))
                .thenReturn(List.of());

        List<JobExecutionResponse> actual = jobExecutionService.getRecentExecutions("DAILY_ANALYTICS");

        assertThat(actual).isEmpty();
    }
}
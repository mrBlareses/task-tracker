package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.model.JobExecutionResponse;
import org.mushroom.repository.JobExecutionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobExecutionRepository jobExecutionRepository;

    public Optional<JobExecutionEntity> tryCreateRunningExecution(String jobName, TriggerType triggerType) {
        JobExecutionEntity execution = JobExecutionEntity.builder()
                .id(UUID.randomUUID())
                .jobName(jobName)
                .status(JobExecutionStatus.RUNNING)
                .triggerType(triggerType)
                .startedAt(OffsetDateTime.now())
                .build();

        try {
            JobExecutionEntity saved = jobExecutionRepository.saveAndFlush(execution);

            log.info("Created RUNNING job execution: id={}, jobName={}, triggerType={}",
                    saved.getId(), saved.getJobName(), saved.getTriggerType());

            return Optional.of(saved);
        } catch (DataIntegrityViolationException ex) {
            log.warn("RUNNING execution already exists for jobName={}, triggerType={}", jobName, triggerType);
            return Optional.empty();
        }
    }

    @Transactional
    public void markSuccess(UUID executionId) {
        JobExecutionEntity execution = jobExecutionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Job execution not found: " + executionId));

        execution.setStatus(JobExecutionStatus.SUCCESS);
        execution.setFinishedAt(OffsetDateTime.now());

        jobExecutionRepository.save(execution);

        log.info("Marked job execution as SUCCESS: id={}", executionId);
    }

    public void deleteExecution(UUID executionId) {
        jobExecutionRepository.deleteById(executionId);
        log.info("Deleted job execution: id={}", executionId);
    }

    @Transactional
    public long deleteStaleRunningExecutions(String jobName, OffsetDateTime threshold) {
        long deleted = jobExecutionRepository.deleteByJobNameAndStatusAndStartedAtBefore(
                jobName,
                JobExecutionStatus.RUNNING,
                threshold
        );

        if (deleted > 0) {
            log.warn("Deleted stale RUNNING executions: jobName={}, count={}, threshold={}",
                    jobName, deleted, threshold);
        }

        return deleted;
    }

    @Transactional(readOnly = true)
    public List<JobExecutionResponse> getRecentExecutions(String jobName) {
        return jobExecutionRepository.findTop20ByJobNameOrderByStartedAtDesc(jobName).stream()
                .map(execution -> JobExecutionResponse.builder()
                        .id(execution.getId())
                        .jobName(execution.getJobName())
                        .status(execution.getStatus())
                        .triggerType(execution.getTriggerType())
                        .startedAt(execution.getStartedAt())
                        .finishedAt(execution.getFinishedAt())
                        .build())
                .toList();
    }
}
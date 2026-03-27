package org.mushroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.repository.JobExecutionRepository;
import org.mushroom.service.JobExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobExecutionServiceImpl implements JobExecutionService {

    private final JobExecutionRepository jobExecutionRepository;

    @Override
    @Transactional
    public JobExecutionEntity createRunningExecution(String jobName, TriggerType triggerType) {
        JobExecutionEntity execution = JobExecutionEntity.builder()
                .id(UUID.randomUUID())
                .jobName(jobName)
                .status(JobExecutionStatus.RUNNING)
                .triggerType(triggerType)
                .startedAt(OffsetDateTime.now())
                .build();

        JobExecutionEntity savedExecution = jobExecutionRepository.save(execution);

        log.info("Created RUNNING job execution: id={}, jobName={}, triggerType={}",
                savedExecution.getId(), savedExecution.getJobName(), savedExecution.getTriggerType());

        return savedExecution;
    }

    @Override
    @Transactional
    public void markSuccess(JobExecutionEntity execution) {
        execution.setStatus(JobExecutionStatus.SUCCESS);
        execution.setFinishedAt(OffsetDateTime.now());
        execution.setErrorMessage(null);

        jobExecutionRepository.save(execution);

        log.info("Marked job execution as SUCCESS: id={}", execution.getId());
    }

    @Override
    @Transactional
    public void markFailed(JobExecutionEntity execution, String errorMessage) {
        execution.setStatus(JobExecutionStatus.FAILED);
        execution.setFinishedAt(OffsetDateTime.now());
        execution.setErrorMessage(errorMessage);

        jobExecutionRepository.save(execution);

        log.info("Marked job execution as FAILED: id={}", execution.getId());
    }
}

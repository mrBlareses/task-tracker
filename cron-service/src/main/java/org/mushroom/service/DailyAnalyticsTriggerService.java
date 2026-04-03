package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.config.SchedulerProperties;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.TriggerType;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAnalyticsTriggerService {

    private static final String JOB_NAME = "DAILY_ANALYTICS";

    private final BackendTriggerService backendTriggerService;
    private final JobExecutionService jobExecutionService;
    private final SchedulerProperties schedulerProperties;

    public void trigger(TriggerType triggerType) {
        log.info("Triggering daily analytics, triggerType={}", triggerType);

        OffsetDateTime staleThreshold = OffsetDateTime.now()
                .minus(schedulerProperties.getStaleThreshold());

        long deletedStale = jobExecutionService.deleteStaleRunningExecutions(
                JOB_NAME,
                staleThreshold
        );

        if (deletedStale > 0) {
            log.warn("Deleted stale RUNNING executions, jobName={}, count={}", JOB_NAME, deletedStale);
        }

        Optional<JobExecutionEntity> executionOptional =
                jobExecutionService.tryCreateRunningExecution(JOB_NAME, triggerType);

        if (executionOptional.isEmpty()) {
            log.warn("Skipping trigger because another execution is already RUNNING, jobName={}, triggerType={}",
                    JOB_NAME, triggerType);
            return;
        }

        JobExecutionEntity execution = executionOptional.get();

        try {
            backendTriggerService.triggerDailyAnalytics();
            jobExecutionService.markSuccess(execution.getId());
            log.info("Daily analytics trigger completed successfully, executionId={}", execution.getId());
        } catch (Exception ex) {
            log.error("Daily analytics trigger failed, executionId={}, error={}",
                    execution.getId(), ex.getMessage(), ex);
            jobExecutionService.deleteExecution(execution.getId());
            throw ex;
        }
    }
}
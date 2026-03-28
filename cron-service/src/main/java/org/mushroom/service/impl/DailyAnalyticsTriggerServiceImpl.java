package org.mushroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.client.BackendTriggerClient;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.mushroom.service.JobExecutionService;
import org.mushroom.entity.JobExecutionEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAnalyticsTriggerServiceImpl implements DailyAnalyticsTriggerService {

    private static final String JOB_NAME = "DAILY_ANALYTICS";
    private static final String SKIP_REASON = "Job execution skipped because another DAILY_ANALYTICS execution is already running";

    private final BackendTriggerClient backendTriggerClient;
    private final JobExecutionService jobExecutionService;

    @Override
    public void trigger(TriggerType triggerType) {

        log.info("Triggering daily analytics, triggerType={}", triggerType);

        if (jobExecutionService.hasRunningExecution(JOB_NAME)) {
            log.warn("Skipping DAILY_ANALYTICS trigger because another execution is already RUNNING, triggerType={}", triggerType);

            jobExecutionService.createSkippedExecution(
                    JOB_NAME,
                    triggerType,
                    SKIP_REASON
            );
            return;
        }

        JobExecutionEntity execution = jobExecutionService.createRunningExecution(
                JOB_NAME,
                triggerType
        );

        try {
            log.info("Calling backend analytics from cron-service, triggerType={}", triggerType);
            backendTriggerClient.triggerDailyAnalytics();
            jobExecutionService.markSuccess(execution);
            log.info("Daily analytics completed successfully, executionId={}", execution.getId());

        } catch (Exception ex) {
            log.error("Daily analytics failed, executionId={}, error={}", execution.getId(), ex.getMessage(), ex);
            jobExecutionService.markFailed(
                    execution,
                    ex.getMessage()
            );
            throw ex;
        }
    }
}
package org.mushroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.client.BackendTriggerClient;
import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.mushroom.service.JobExecutionService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAnalyticsTriggerServiceImpl implements DailyAnalyticsTriggerService {

    private static final String JOB_NAME = "DAILY_ANALYTICS";

    private final BackendTriggerClient backendTriggerClient;
    private final JobExecutionService jobExecutionService;

    @Override
    public void trigger() {
        JobExecutionEntity execution = jobExecutionService.createRunningExecution(JOB_NAME, TriggerType.CRON);

        try {
            log.info("Triggering daily analytics via BackendTriggerClient");
            backendTriggerClient.triggerDailyAnalytics();
            jobExecutionService.markSuccess(execution);
        } catch (Exception ex) {
            jobExecutionService.markFailed(execution, ex.getMessage());
            throw ex;
        }
    }
}

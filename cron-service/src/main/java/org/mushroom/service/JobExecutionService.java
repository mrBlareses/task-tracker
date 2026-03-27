package org.mushroom.service;

import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.TriggerType;

public interface JobExecutionService {
    JobExecutionEntity createRunningExecution(String jobName, TriggerType triggerType);
    void markSuccess(JobExecutionEntity execution);
    void markFailed(JobExecutionEntity execution, String errorMessage);
}

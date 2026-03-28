package org.mushroom.service;

import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.TriggerType;

public interface JobExecutionService {
    JobExecutionEntity createRunningExecution(String jobName, TriggerType triggerType);
    JobExecutionEntity createSkippedExecution(String jobName, TriggerType triggerType, String reason);
    boolean hasRunningExecution(String jobName);
    void markSuccess(JobExecutionEntity execution);
    void markFailed(JobExecutionEntity execution, String errorMessage);
}

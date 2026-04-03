package org.mushroom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.mushroom.entity.enums.TriggerType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionResponse {
    private UUID id;
    private String jobName;
    private JobExecutionStatus status;
    private TriggerType triggerType;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
}
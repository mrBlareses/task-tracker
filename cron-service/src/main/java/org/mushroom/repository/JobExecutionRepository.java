package org.mushroom.repository;

import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {

    List<JobExecutionEntity> findTop20ByJobNameOrderByStartedAtDesc(String jobName);

    long deleteByJobNameAndStatusAndStartedAtBefore(
            String jobName,
            JobExecutionStatus status,
            OffsetDateTime threshold
    );
}

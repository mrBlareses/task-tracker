package org.mushroom.repository;

import org.mushroom.entity.JobExecutionEntity;
import org.mushroom.entity.enums.JobExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {
    boolean existsByJobNameAndStatus(String jobName, JobExecutionStatus status);
}

package org.mushroom.repository;

import org.mushroom.entity.JobExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {
}

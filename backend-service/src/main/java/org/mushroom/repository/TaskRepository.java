package org.mushroom.repository;

import jakarta.persistence.LockModeType;
import org.mushroom.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.user.id = :userId")
    Optional<Task> findByIdAndUserIdWithLock(@Param("id") UUID id, @Param("userId") UUID userId);
}

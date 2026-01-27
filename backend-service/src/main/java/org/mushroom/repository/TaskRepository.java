package org.mushroom.repository;

import org.mushroom.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query(value = """
            UPDATE tasks
            SET
                title = COALESCE(:title, title),
                description = COALESCE(:description, description),
                status = COALESCE(:status, status),
                updated_at = :updatedAt
            WHERE id = :id
            RETURNING id, title, description, status, user_id, created_at, updated_at
            """, nativeQuery = true)
    Optional<Task> update(@Param("title") String title,
                          @Param("description") String description,
                          @Param("status") String status,
                          @Param("updatedAt") OffsetDateTime updatedAt);
}
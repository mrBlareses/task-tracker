package org.mushroom.repository;

import org.mushroom.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                UPDATE tasks
                SET title = :title,
                    description = :description,
                    status = :status,
                    updated_at = :updatedAt
                WHERE id = :id
            """, nativeQuery = true)
    int updateTaskById(@Param("id") UUID id,
                       @Param("title") String title,
                       @Param("description") String description,
                       @Param("status") String status,
                       @Param("updatedAt") OffsetDateTime updatedAt);
}
package org.mushroom.repository;

import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEmailRepository extends JpaRepository<OutboxEmail, Long> {

    List<OutboxEmail> findByStatusAndNextAttemptAtBefore(
            OutboxEmailStatus status, LocalDateTime nextAttemptAt
    );
}
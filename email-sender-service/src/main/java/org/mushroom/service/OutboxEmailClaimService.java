package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEmailClaimService {

    private final OutboxEmailRepository outboxEmailRepository;

    @Transactional
    public List<Long> claimNextBatch(int batchSize) throws OutboxClaimException {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Long> ids = outboxEmailRepository.lockNextBatchIds(now, batchSize);

            if (ids.isEmpty()) {
                return List.of();
            }

            outboxEmailRepository.updateStatusByIds(ids, OutboxEmailStatus.PROCESSING, now);

            return ids;
        } catch (Exception ex) {
            throw new OutboxClaimException("Failed to claim outbox batch", ex);
        }
    }
}
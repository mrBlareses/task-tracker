package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEmailClaimService {

    private final OutboxEmailRepository outboxEmailRepository;

    @Value("${email-processing.batch-size:50}")
    private int batchSize;

    @Value("${email-processing.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public List<Long> claimNextBatch() throws OutboxClaimException {
        try {
            List<Long> ids = outboxEmailRepository.lockNextBatchIds(batchSize, maxAttempts);

            if (ids.isEmpty()) {
                return List.of();
            }

            outboxEmailRepository.updateStatusByIds(ids, OutboxEmailStatus.PROCESSING);

            return ids;
        } catch (Exception ex) {
            throw new OutboxClaimException("Failed to claim outbox batch", ex);
        }
    }
}
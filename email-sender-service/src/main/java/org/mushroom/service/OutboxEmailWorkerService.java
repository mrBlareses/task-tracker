package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.exception.OutboxClaimException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEmailWorkerService {

    private final OutboxEmailClaimService outboxEmailClaimService;
    private final AsyncOutboxEmailProcessor asyncOutboxEmailProcessor;

    public void processBatch() throws OutboxClaimException {
        List<OutboxEmail> emails = outboxEmailClaimService.claimNextBatch();

        if (emails.isEmpty()) {
            return;
        }

        emails.forEach(asyncOutboxEmailProcessor::processAsync);
    }
}
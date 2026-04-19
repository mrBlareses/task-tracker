package org.mushroom.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.service.OutboxEmailWorkerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEmailProcessingJob {

    private final OutboxEmailWorkerService outboxEmailWorkerService;

    @Scheduled(fixedDelayString = "${email-processing.fixed-delay-ms:5000}")
    public void processPendingEmails() {
        try {
            outboxEmailWorkerService.processBatch();
        } catch (OutboxClaimException ex) {
            log.error("Failed to claim outbox batch", ex);
        }
    }
}
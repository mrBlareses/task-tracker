package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEmailWorkerService {

    private final OutboxEmailClaimService outboxEmailClaimService;
    private final SingleOutboxEmailProcessor singleOutboxEmailProcessor;

    @Qualifier("emailProcessingExecutor")
    private final ThreadPoolTaskExecutor emailProcessingExecutor;

    public void processBatch() throws OutboxClaimException {
        List<OutboxEmail> emails = outboxEmailClaimService.claimNextBatch();

        if (emails.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = emails.stream()
                .map(email -> CompletableFuture.runAsync(() -> {
                    try {
                        singleOutboxEmailProcessor.processOne(email);
                    } catch (OutboxEmailProcessingException ex) {
                        log.error("Failed to process outbox email id={}", email.getId(), ex);
                    }
                }, emailProcessingExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
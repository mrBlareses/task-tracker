package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${email-processing.batch-size:50}")
    private int batchSize;

    @Value("${email-processing.retry-delay-minutes:5}")
    private long retryDelayMinutes;

    public void processBatch() throws OutboxClaimException {
        List<Long> ids = outboxEmailClaimService.claimNextBatch(batchSize);

        if (ids.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = ids.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        singleOutboxEmailProcessor.processOne(id, retryDelayMinutes);
                    } catch (OutboxEmailProcessingException ex) {
                        log.error("Failed to process outbox email id={}", id, ex);
                    }
                }, emailProcessingExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
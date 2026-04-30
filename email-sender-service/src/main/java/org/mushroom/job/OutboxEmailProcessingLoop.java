package org.mushroom.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.service.OutboxEmailWorkerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEmailProcessingLoop implements SmartLifecycle {

    private final OutboxEmailWorkerService outboxEmailWorkerService;

    @Value("${email-processing.fixed-delay-ms:5000}")
    private long idleDelayMs;

    private volatile boolean running;
    private ExecutorService loopExecutor;
    private Future<?> loopTask;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        loopExecutor = Executors.newSingleThreadExecutor(
                Thread.ofVirtual()
                        .name("outbox-processing-loop-", 0)
                        .factory()
        );

        loopTask = loopExecutor.submit(this::runLoop);

        log.info("Outbox email processing loop started");
    }

    private void runLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                int claimedCount = outboxEmailWorkerService.processBatch();

                if (claimedCount == 0) {
                    sleepQuietly();
                }
            } catch (OutboxClaimException ex) {
                log.error("Failed to claim outbox batch", ex);
                sleepQuietly();
            } catch (Exception ex) {
                log.error("Unexpected error in outbox processing loop", ex);
                sleepQuietly();
            }
        }
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(idleDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        running = false;

        if (loopTask != null) {
            loopTask.cancel(true);
            loopTask = null;
        }

        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
            loopExecutor = null;
        }

        log.info("Outbox email processing loop stopped");
    }

    @Override
    public synchronized void stop(@NonNull Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
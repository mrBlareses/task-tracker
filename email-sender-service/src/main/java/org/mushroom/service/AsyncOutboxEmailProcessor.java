package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncOutboxEmailProcessor {

    private final SingleOutboxEmailProcessor singleOutboxEmailProcessor;

    @Async("emailProcessingExecutor")
    public void processAsync(OutboxEmail email) {
        try {
            singleOutboxEmailProcessor.processOne(email);
        } catch (OutboxEmailProcessingException ex) {
            log.error("Failed to process outbox email id={}", email.getId(), ex);
        }
    }
}
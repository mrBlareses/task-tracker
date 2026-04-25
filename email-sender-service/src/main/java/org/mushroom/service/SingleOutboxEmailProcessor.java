package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.EmailDispatchException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SingleOutboxEmailProcessor {

    private final OutboxEmailRepository outboxEmailRepository;
    private final EmailDispatcher emailDispatcher;

    @Value("${email-processing.retry-delay-minutes:5}")
    private long retryDelayMinutes;

    @Transactional
    public void processOne(OutboxEmail email) throws OutboxEmailProcessingException {
        LocalDateTime now = LocalDateTime.now();

        try {
            emailDispatcher.send(email);

            email.setStatus(OutboxEmailStatus.SENT);
            email.setUpdatedAt(now);

            outboxEmailRepository.save(email);
        } catch (EmailDispatchException ex) {
            email.setStatus(OutboxEmailStatus.FAILED);
            email.setAttempts(email.getAttempts() + 1);
            email.setNextAttemptAt(now.plusMinutes(retryDelayMinutes));
            email.setUpdatedAt(now);

            outboxEmailRepository.save(email);
        } catch (Exception ex) {
            throw new OutboxEmailProcessingException(
                    "Failed to process outbox email id=" + email.getId(),
                    ex
            );
        }
    }
}
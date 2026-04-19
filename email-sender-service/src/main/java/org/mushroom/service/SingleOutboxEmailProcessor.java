package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.EmailDispatchException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SingleOutboxEmailProcessor {

    private final OutboxEmailRepository outboxEmailRepository;
    private final EmailDispatcher emailDispatcher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(Long emailId, long retryDelayMinutes) throws OutboxEmailProcessingException {
        OutboxEmail email = outboxEmailRepository.findById(emailId)
                .orElseThrow(() -> new OutboxEmailProcessingException("Outbox email not found: " + emailId));

        try {
            emailDispatcher.send(email);

            email.setStatus(OutboxEmailStatus.SENT);
            email.setUpdatedAt(LocalDateTime.now());

            outboxEmailRepository.save(email);
        } catch (EmailDispatchException ex) {
            email.setStatus(OutboxEmailStatus.FAILED);
            email.setAttempts(email.getAttempts() + 1);
            email.setNextAttemptAt(LocalDateTime.now().plusMinutes(retryDelayMinutes));
            email.setUpdatedAt(LocalDateTime.now());

            outboxEmailRepository.save(email);
        } catch (Exception ex) {
            throw new OutboxEmailProcessingException("Failed to process outbox email id=" + emailId, ex);
        }
    }
}
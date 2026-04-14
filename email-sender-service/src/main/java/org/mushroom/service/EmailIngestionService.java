package org.mushroom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.EmailRequestedEventValidator;
import org.mushroom.model.EmailRequestedEvent;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailIngestionService {

    private final OutboxEmailRepository outboxEmailRepository;
    private final EmailRequestedEventValidator emailRequestedEventValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public boolean ingest(EmailRequestedEvent event) {
        String dedupKey = emailRequestedEventValidator.validateAndBuildDedupKey(event);

        LocalDateTime now = OffsetDateTime.now().toLocalDateTime();

        JsonNode payloadNode = objectMapper.valueToTree(
                event.getPayload() != null ? event.getPayload() : Collections.emptyMap()
        );

        OutboxEmail entity = OutboxEmail.builder()
                .payload(payloadNode)
                .status(OutboxEmailStatus.NEW)
                .type(event.getType())
                .dedupKey(dedupKey)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            outboxEmailRepository.saveAndFlush(entity);

            log.info("Outbox email saved successfully: id={}, dedupKey={}, type={}, status={}",
                    entity.getId(), entity.getDedupKey(), entity.getType(), entity.getStatus());

            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
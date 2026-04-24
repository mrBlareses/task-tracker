package org.mushroom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.contract.avro.UserDailyReportEventAvro;
import org.mushroom.contract.avro.UserRegisteredEventAvro;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailEventDeserializationException;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.exception.UserDailyReportEventValidator;
import org.mushroom.exception.UserRegisteredEventValidator;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailIngestionService {

    private final OutboxEmailRepository outboxEmailRepository;
    private final UserRegisteredEventValidator userRegisteredEventValidator;
    private final UserDailyReportEventValidator userDailyReportEventValidator;
    private final ObjectMapper objectMapper;

    @Transactional
    public void ingestUserRegistered(UserRegisteredEventAvro event)
            throws EmailEventValidationException, EmailEventDeserializationException {

        String dedupKey = userRegisteredEventValidator.validateAndBuildDedupKey(event);

        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("id", event.getId());
        payloadNode.put("email", event.getEmail());
        payloadNode.put("firstName", event.getFirstName());
        payloadNode.put("lastName", event.getLastName());

        saveOutboxEmail(payloadNode, dedupKey, OutboxEmailType.WELCOME);
    }

    @Transactional
    public void ingestDailyReport(UserDailyReportEventAvro event)
            throws EmailEventValidationException, EmailEventDeserializationException {

        String dedupKey = userDailyReportEventValidator.validateAndBuildDedupKey(event);

        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("userId", event.getUserId());
        payloadNode.put("email", event.getEmail());
        payloadNode.put("pendingCount", event.getPendingCount());
        payloadNode.put("completedCount", event.getCompletedCount());

        saveOutboxEmail(payloadNode, dedupKey, OutboxEmailType.REPORT);
    }

    protected void saveOutboxEmail(ObjectNode payloadNode, String dedupKey, OutboxEmailType type)
            throws EmailEventDeserializationException {

        LocalDateTime now = LocalDateTime.now();
        String payloadJson = toJson(payloadNode);

        int insertedRows = outboxEmailRepository.insertIgnoreConflict(
                payloadJson,
                OutboxEmailStatus.NEW.name(),
                type.name(),
                dedupKey,
                0,
                now,
                now,
                now
        );

        if (insertedRows == 1) {
            log.info("Outbox email saved successfully: dedupKey={}, type={}, status={}",
                    dedupKey, type, OutboxEmailStatus.NEW);
        }
    }

    private String toJson(ObjectNode payloadNode) throws EmailEventDeserializationException {
        try {
            return objectMapper.writeValueAsString(payloadNode);
        } catch (JsonProcessingException ex) {
            throw new EmailEventDeserializationException(
                    "Failed to serialize payload to json",
                    ex
            );
        }
    }
}
package org.mushroom.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailEventDeserializationException;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.exception.UserDailyReportEventValidator;
import org.mushroom.exception.UserRegisteredEventValidator;
import org.mushroom.model.UserDailyReportEvent;
import org.mushroom.model.UserRegisteredEvent;
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
    public void ingestUserRegistered(JsonNode kafkaEventNode)
            throws EmailEventValidationException, EmailEventDeserializationException {

        JsonNode payloadNode = extractPayloadNode(kafkaEventNode);
        UserRegisteredEvent event = toUserRegisteredEvent(payloadNode);
        String dedupKey = userRegisteredEventValidator.validateAndBuildDedupKey(event);

        saveOutboxEmail(payloadNode, dedupKey, OutboxEmailType.WELCOME);
    }

    @Transactional
    public void ingestDailyReport(JsonNode kafkaEventNode)
            throws EmailEventValidationException, EmailEventDeserializationException {

        JsonNode payloadNode = extractPayloadNode(kafkaEventNode);
        UserDailyReportEvent event = toUserDailyReportEvent(payloadNode);
        String dedupKey = userDailyReportEventValidator.validateAndBuildDedupKey(event);

        saveOutboxEmail(payloadNode, dedupKey, OutboxEmailType.REPORT);
    }

    protected void saveOutboxEmail(JsonNode payloadNode, String dedupKey, OutboxEmailType type)
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

    private JsonNode extractPayloadNode(JsonNode kafkaEventNode) throws EmailEventValidationException {
        if (kafkaEventNode == null || kafkaEventNode.isNull()) {
            throw new EmailEventValidationException("event is null");
        }

        JsonNode payloadNode = kafkaEventNode.get("payload");
        if (payloadNode == null || payloadNode.isNull() || payloadNode.isMissingNode()) {
            throw new EmailEventValidationException("payload is empty");
        }

        return payloadNode;
    }

    private UserRegisteredEvent toUserRegisteredEvent(JsonNode payloadNode)
            throws EmailEventDeserializationException {
        try {
            return objectMapper.treeToValue(payloadNode, UserRegisteredEvent.class);
        } catch (JsonProcessingException ex) {
            throw new EmailEventDeserializationException(
                    "Failed to deserialize user-registered payload",
                    ex
            );
        }
    }

    private UserDailyReportEvent toUserDailyReportEvent(JsonNode payloadNode)
            throws EmailEventDeserializationException {
        try {
            return objectMapper.treeToValue(payloadNode, UserDailyReportEvent.class);
        } catch (JsonProcessingException ex) {
            throw new EmailEventDeserializationException(
                    "Failed to deserialize daily-report payload",
                    ex
            );
        }
    }

    private String toJson(JsonNode payloadNode) throws EmailEventDeserializationException {
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
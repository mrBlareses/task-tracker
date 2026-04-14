package org.mushroom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailRequestedEventValidator;
import org.mushroom.model.EmailRequestedEvent;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailIngestionServiceUnitTest {

    @Mock
    private OutboxEmailRepository outboxEmailRepository;

    @Mock
    private EmailRequestedEventValidator emailRequestedEventValidator;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EmailIngestionService emailIngestionService;

    @Test
    @DisplayName("ingest — сохраняет новую запись в outbox и возвращает true для валидного события")
    void ingest_ShouldSaveNewOutboxEmailAndReturnTrue_WhenEventIsValid() {
        Map<String, Object> payload = Map.of(
                "email", "user1@test.local",
                "id", 1,
                "username", "user1"
        );

        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(payload)
                .build();

        when(emailRequestedEventValidator.validateAndBuildDedupKey(event))
                .thenReturn("user1@test.local:1");

        boolean result = emailIngestionService.ingest(event);

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        Invocation saveInvocation = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("saveAndFlush"))
                .findFirst()
                .orElse(null);

        assertThat(result).isTrue();
        assertThat(saveInvocation).isNotNull();

        OutboxEmail savedEntity = saveInvocation.getArgument(0);
        JsonNode expectedPayload = objectMapper.valueToTree(payload);

        assertThat(savedEntity.getPayload()).isEqualTo(expectedPayload);
        assertThat(savedEntity.getStatus()).isEqualTo(OutboxEmailStatus.NEW);
        assertThat(savedEntity.getType()).isEqualTo(OutboxEmailType.WELCOME);
        assertThat(savedEntity.getDedupKey()).isEqualTo("user1@test.local:1");
        assertThat(savedEntity.getAttempts()).isEqualTo(0);
        assertThat(savedEntity.getNextAttemptAt()).isNotNull();
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("ingest — возвращает false, если duplicate пойман на уровне DB constraint")
    void ingest_ShouldReturnFalse_WhenDuplicateDetectedByDatabaseConstraint() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        when(emailRequestedEventValidator.validateAndBuildDedupKey(event))
                .thenReturn("user1@test.local:1");

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(outboxEmailRepository)
                .saveAndFlush(any(OutboxEmail.class));

        boolean result = emailIngestionService.ingest(event);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("ingest — пробрасывает исключение validator-а и не сохраняет запись")
    void ingest_ShouldThrowException_WhenValidatorFails() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        when(emailRequestedEventValidator.validateAndBuildDedupKey(event))
                .thenThrow(new IllegalArgumentException("payload.email is blank"));

        assertThatThrownBy(() -> emailIngestionService.ingest(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload.email is blank");

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        boolean hasSaveCall = invocations.stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("saveAndFlush"));

        assertThat(hasSaveCall).isFalse();
    }
}
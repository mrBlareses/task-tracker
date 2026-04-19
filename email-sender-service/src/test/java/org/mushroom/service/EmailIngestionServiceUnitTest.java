package org.mushroom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.exception.UserDailyReportEventValidator;
import org.mushroom.exception.UserRegisteredEventValidator;
import org.mushroom.repository.OutboxEmailRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailIngestionServiceUnitTest {

    @Mock
    private OutboxEmailRepository outboxEmailRepository;

    @Mock
    private UserRegisteredEventValidator userRegisteredEventValidator;

    @Mock
    private UserDailyReportEventValidator userDailyReportEventValidator;

    private ObjectMapper objectMapper;
    private EmailIngestionService emailIngestionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emailIngestionService = new EmailIngestionService(
                outboxEmailRepository,
                userRegisteredEventValidator,
                userDailyReportEventValidator,
                objectMapper
        );
    }

    @Test
    @DisplayName("ingestUserRegistered — вставляет новую запись WELCOME через ON CONFLICT DO NOTHING")
    void ingestUserRegistered_ShouldInsertWelcomeOutboxEmail_WhenEventIsValid() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "id", "1",
                        "email", "user1@test.local",
                        "firstName", "John",
                        "lastName", "Doe"
                )
        );

        when(userRegisteredEventValidator.validateAndBuildDedupKey(any()))
                .thenReturn("user1@test.local:1");

        when(outboxEmailRepository.insertIgnoreConflict(
                any(),
                any(),
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any()
        )).thenReturn(1);

        emailIngestionService.ingestUserRegistered(kafkaEventNode);

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        Invocation insertInvocation = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("insertIgnoreConflict"))
                .findFirst()
                .orElse(null);

        assertThat(insertInvocation).isNotNull();

        String payload = insertInvocation.getArgument(0, String.class);
        String status = insertInvocation.getArgument(1, String.class);
        String type = insertInvocation.getArgument(2, String.class);
        String dedupKey = insertInvocation.getArgument(3, String.class);
        Integer attempts = insertInvocation.getArgument(4, Integer.class);
        LocalDateTime nextAttemptAt = insertInvocation.getArgument(5, LocalDateTime.class);
        LocalDateTime createdAt = insertInvocation.getArgument(6, LocalDateTime.class);
        LocalDateTime updatedAt = insertInvocation.getArgument(7, LocalDateTime.class);

        assertThat(payload).contains("user1@test.local");
        assertThat(status).isEqualTo("NEW");
        assertThat(type).isEqualTo("WELCOME");
        assertThat(dedupKey).isEqualTo("user1@test.local:1");
        assertThat(attempts).isEqualTo(0);
        assertThat(nextAttemptAt).isNotNull();
        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();
    }

    @Test
    @DisplayName("ingestDailyReport — вставляет новую запись REPORT через ON CONFLICT DO NOTHING")
    void ingestDailyReport_ShouldInsertReportOutboxEmail_WhenEventIsValid() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "userId", "2",
                        "email", "user2@test.local",
                        "pendingCount", 3,
                        "completedCount", 5
                )
        );

        when(userDailyReportEventValidator.validateAndBuildDedupKey(any()))
                .thenReturn("user2@test.local:2");

        when(outboxEmailRepository.insertIgnoreConflict(
                any(),
                any(),
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any()
        )).thenReturn(1);

        emailIngestionService.ingestDailyReport(kafkaEventNode);

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        Invocation insertInvocation = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("insertIgnoreConflict"))
                .findFirst()
                .orElse(null);

        assertThat(insertInvocation).isNotNull();

        String payload = insertInvocation.getArgument(0, String.class);
        String status = insertInvocation.getArgument(1, String.class);
        String type = insertInvocation.getArgument(2, String.class);
        String dedupKey = insertInvocation.getArgument(3, String.class);
        Integer attempts = insertInvocation.getArgument(4, Integer.class);
        LocalDateTime nextAttemptAt = insertInvocation.getArgument(5, LocalDateTime.class);
        LocalDateTime createdAt = insertInvocation.getArgument(6, LocalDateTime.class);
        LocalDateTime updatedAt = insertInvocation.getArgument(7, LocalDateTime.class);

        assertThat(payload).contains("user2@test.local");
        assertThat(status).isEqualTo("NEW");
        assertThat(type).isEqualTo("REPORT");
        assertThat(dedupKey).isEqualTo("user2@test.local:2");
        assertThat(attempts).isEqualTo(0);
        assertThat(nextAttemptAt).isNotNull();
        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();
    }

    @Test
    @DisplayName("ingestUserRegistered — duplicate не ломает обработку и не кидает исключение")
    void ingestUserRegistered_ShouldNotThrow_WhenDuplicateDetected() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "id", "1",
                        "email", "user1@test.local",
                        "firstName", "John",
                        "lastName", "Doe"
                )
        );

        when(userRegisteredEventValidator.validateAndBuildDedupKey(any()))
                .thenReturn("user1@test.local:1");

        when(outboxEmailRepository.insertIgnoreConflict(
                any(),
                any(),
                any(),
                any(),
                any(Integer.class),
                any(),
                any(),
                any()
        )).thenReturn(0);

        emailIngestionService.ingestUserRegistered(kafkaEventNode);

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        long insertCalls = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("insertIgnoreConflict"))
                .count();

        assertThat(insertCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("ingestUserRegistered — пробрасывает EmailEventValidationException если событие невалидно")
    void ingestUserRegistered_ShouldThrow_WhenValidatorFails() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "id", "1",
                        "email", "user1@test.local"
                )
        );

        when(userRegisteredEventValidator.validateAndBuildDedupKey(any()))
                .thenThrow(new EmailEventValidationException("payload.id is blank"));

        assertThatThrownBy(() -> emailIngestionService.ingestUserRegistered(kafkaEventNode))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.id is blank");
    }

    @Test
    @DisplayName("ingestDailyReport — пробрасывает EmailEventValidationException если событие невалидно")
    void ingestDailyReport_ShouldThrow_WhenValidatorFails() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "userId", "2",
                        "email", "user2@test.local"
                )
        );

        when(userDailyReportEventValidator.validateAndBuildDedupKey(any()))
                .thenThrow(new EmailEventValidationException("payload.userId is blank"));

        assertThatThrownBy(() -> emailIngestionService.ingestDailyReport(kafkaEventNode))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.userId is blank");
    }

    private JsonNode buildKafkaEventNode(Map<String, Object> payload) {
        return objectMapper.valueToTree(
                Map.of("payload", payload)
        );
    }
}
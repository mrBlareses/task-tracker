package org.mushroom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.contract.avro.UserDailyReportEventAvro;
import org.mushroom.contract.avro.UserRegisteredEventAvro;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.exception.UserDailyReportEventValidator;
import org.mushroom.exception.UserRegisteredEventValidator;
import org.mushroom.repository.OutboxEmailRepository;

import java.time.LocalDateTime;
import java.util.Collection;

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
        UserRegisteredEventAvro event = UserRegisteredEventAvro.newBuilder()
                .setId("1")
                .setEmail("user1@test.local")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        when(userRegisteredEventValidator.validateAndBuildDedupKey(any(UserRegisteredEventAvro.class)))
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

        emailIngestionService.ingestUserRegistered(event);

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
        UserDailyReportEventAvro event = UserDailyReportEventAvro.newBuilder()
                .setUserId("2")
                .setEmail("user2@test.local")
                .setPendingCount(3)
                .setCompletedCount(5)
                .build();

        when(userDailyReportEventValidator.validateAndBuildDedupKey(any(UserDailyReportEventAvro.class)))
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

        emailIngestionService.ingestDailyReport(event);

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
        UserRegisteredEventAvro event = UserRegisteredEventAvro.newBuilder()
                .setId("1")
                .setEmail("user1@test.local")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        when(userRegisteredEventValidator.validateAndBuildDedupKey(any(UserRegisteredEventAvro.class)))
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

        emailIngestionService.ingestUserRegistered(event);

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        long insertCalls = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("insertIgnoreConflict"))
                .count();

        assertThat(insertCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("ingestUserRegistered — пробрасывает EmailEventValidationException если событие невалидно")
    void ingestUserRegistered_ShouldThrow_WhenValidatorFails() throws Exception {
        UserRegisteredEventAvro event = UserRegisteredEventAvro.newBuilder()
                .setId("1")
                .setEmail("user1@test.local")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        when(userRegisteredEventValidator.validateAndBuildDedupKey(any(UserRegisteredEventAvro.class)))
                .thenThrow(new EmailEventValidationException("payload.id is blank"));

        assertThatThrownBy(() -> emailIngestionService.ingestUserRegistered(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.id is blank");
    }

    @Test
    @DisplayName("ingestDailyReport — пробрасывает EmailEventValidationException если событие невалидно")
    void ingestDailyReport_ShouldThrow_WhenValidatorFails() throws Exception {
        UserDailyReportEventAvro event = UserDailyReportEventAvro.newBuilder()
                .setUserId("2")
                .setEmail("user2@test.local")
                .setPendingCount(3)
                .setCompletedCount(5)
                .build();

        when(userDailyReportEventValidator.validateAndBuildDedupKey(any(UserDailyReportEventAvro.class)))
                .thenThrow(new EmailEventValidationException("payload.userId is blank"));

        assertThatThrownBy(() -> emailIngestionService.ingestDailyReport(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.userId is blank");
    }
}
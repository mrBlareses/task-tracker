package org.mushroom.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.service.EmailIngestionService;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

@ExtendWith(MockitoExtension.class)
class EmailRequestKafkaListenerUnitTest {

    @Mock
    private EmailIngestionService emailIngestionService;

    @Mock
    private Acknowledgment acknowledgment;

    private ObjectMapper objectMapper;
    private EmailRequestKafkaListener emailRequestKafkaListener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emailRequestKafkaListener = new EmailRequestKafkaListener(emailIngestionService);
    }

    @Test
    @DisplayName("listenUserRegistered — вызывает acknowledge при успешной обработке")
    void listenUserRegistered_ShouldAcknowledge_WhenProcessingSucceeded() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "id", "1",
                        "email", "user1@test.local",
                        "firstName", "John",
                        "lastName", "Doe"
                )
        );

        ConsumerRecord<String, JsonNode> record =
                new ConsumerRecord<>("user-registered", 0, 15L, "user1@test.local", kafkaEventNode);

        emailRequestKafkaListener.listenUserRegistered(record, acknowledgment);

        Collection<Invocation> acknowledgmentInvocations = mockingDetails(acknowledgment).getInvocations();

        long acknowledgeCalls = acknowledgmentInvocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("acknowledge"))
                .count();

        assertThat(acknowledgeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("listenUserRegistered — не вызывает acknowledge если ingest выбрасывает исключение")
    void listenUserRegistered_ShouldNotAcknowledge_WhenProcessingFailed() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "id", "1",
                        "email", "user1@test.local",
                        "firstName", "John",
                        "lastName", "Doe"
                )
        );

        ConsumerRecord<String, JsonNode> record =
                new ConsumerRecord<>("user-registered", 0, 16L, "user1@test.local", kafkaEventNode);

        doThrow(new EmailEventValidationException("payload.id is blank"))
                .when(emailIngestionService)
                .ingestUserRegistered(any(JsonNode.class));

        assertThatThrownBy(() -> emailRequestKafkaListener.listenUserRegistered(record, acknowledgment))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.id is blank");

        Collection<Invocation> acknowledgmentInvocations = mockingDetails(acknowledgment).getInvocations();

        long acknowledgeCalls = acknowledgmentInvocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("acknowledge"))
                .count();

        assertThat(acknowledgeCalls).isEqualTo(0);
    }

    @Test
    @DisplayName("listenDailyReport — вызывает acknowledge при успешной обработке")
    void listenDailyReport_ShouldAcknowledge_WhenProcessingSucceeded() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "userId", "2",
                        "email", "user2@test.local",
                        "pendingCount", 3,
                        "completedCount", 5
                )
        );

        ConsumerRecord<String, JsonNode> record =
                new ConsumerRecord<>("daily-report", 1, 20L, "2", kafkaEventNode);

        emailRequestKafkaListener.listenDailyReport(record, acknowledgment);

        Collection<Invocation> acknowledgmentInvocations = mockingDetails(acknowledgment).getInvocations();

        long acknowledgeCalls = acknowledgmentInvocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("acknowledge"))
                .count();

        assertThat(acknowledgeCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("listenDailyReport — не вызывает acknowledge если ingest выбрасывает исключение")
    void listenDailyReport_ShouldNotAcknowledge_WhenProcessingFailed() throws Exception {
        JsonNode kafkaEventNode = buildKafkaEventNode(
                Map.of(
                        "userId", "2",
                        "email", "user2@test.local",
                        "pendingCount", 3,
                        "completedCount", 5
                )
        );

        ConsumerRecord<String, JsonNode> record =
                new ConsumerRecord<>("daily-report", 1, 21L, "2", kafkaEventNode);

        doThrow(new EmailEventValidationException("payload.userId is blank"))
                .when(emailIngestionService)
                .ingestDailyReport(any(JsonNode.class));

        assertThatThrownBy(() -> emailRequestKafkaListener.listenDailyReport(record, acknowledgment))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.userId is blank");

        Collection<Invocation> acknowledgmentInvocations = mockingDetails(acknowledgment).getInvocations();

        long acknowledgeCalls = acknowledgmentInvocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("acknowledge"))
                .count();

        assertThat(acknowledgeCalls).isEqualTo(0);
    }

    private JsonNode buildKafkaEventNode(Map<String, Object> payload) {
        return objectMapper.valueToTree(
                Map.of("payload", payload)
        );
    }
}
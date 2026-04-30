package org.mushroom.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import org.mushroom.service.EmailIngestionService;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Collection;

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

    private EmailRequestKafkaListener emailRequestKafkaListener;

    @BeforeEach
    void setUp() {
        emailRequestKafkaListener = new EmailRequestKafkaListener(emailIngestionService);
    }

    @Test
    @DisplayName("listenUserRegistered — вызывает acknowledge при успешной обработке")
    void listenUserRegistered_ShouldAcknowledge_WhenProcessingSucceeded() throws Exception {
        UserRegisteredEventAvro event = UserRegisteredEventAvro.newBuilder()
                .setId("1")
                .setEmail("user1@test.local")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        ConsumerRecord<String, UserRegisteredEventAvro> record =
                new ConsumerRecord<>("user-registered", 0, 15L, "user1@test.local", event);

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
        UserRegisteredEventAvro event = UserRegisteredEventAvro.newBuilder()
                .setId("1")
                .setEmail("user1@test.local")
                .setFirstName("John")
                .setLastName("Doe")
                .build();

        ConsumerRecord<String, UserRegisteredEventAvro> record =
                new ConsumerRecord<>("user-registered", 0, 16L, "user1@test.local", event);

        doThrow(new EmailEventValidationException("payload.id is blank"))
                .when(emailIngestionService)
                .ingestUserRegistered(any(UserRegisteredEventAvro.class));

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
        UserDailyReportEventAvro event = UserDailyReportEventAvro.newBuilder()
                .setUserId("2")
                .setEmail("user2@test.local")
                .setPendingCount(3)
                .setCompletedCount(5)
                .build();

        ConsumerRecord<String, UserDailyReportEventAvro> record =
                new ConsumerRecord<>("daily-report", 1, 20L, "2", event);

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
        UserDailyReportEventAvro event = UserDailyReportEventAvro.newBuilder()
                .setUserId("2")
                .setEmail("user2@test.local")
                .setPendingCount(3)
                .setCompletedCount(5)
                .build();

        ConsumerRecord<String, UserDailyReportEventAvro> record =
                new ConsumerRecord<>("daily-report", 1, 21L, "2", event);

        doThrow(new EmailEventValidationException("payload.userId is blank"))
                .when(emailIngestionService)
                .ingestDailyReport(any(UserDailyReportEventAvro.class));

        assertThatThrownBy(() -> emailRequestKafkaListener.listenDailyReport(record, acknowledgment))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.userId is blank");

        Collection<Invocation> acknowledgmentInvocations = mockingDetails(acknowledgment).getInvocations();

        long acknowledgeCalls = acknowledgmentInvocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("acknowledge"))
                .count();

        assertThat(acknowledgeCalls).isEqualTo(0);
    }
}
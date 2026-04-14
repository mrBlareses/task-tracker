package org.mushroom.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.model.EmailRequestedEvent;
import org.mushroom.service.EmailIngestionService;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailRequestKafkaListenerUnitTest {

    @Mock
    private EmailIngestionService emailIngestionService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private EmailRequestKafkaListener emailRequestKafkaListener;

    @Test
    @DisplayName("listen — вызывает acknowledge, если ingest вернул true")
    void listen_ShouldAcknowledge_WhenIngestReturnedTrue() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        ConsumerRecord<String, EmailRequestedEvent> record =
                new ConsumerRecord<>("user-registered", 0, 0L, "key-1", event);

        AtomicInteger acknowledgeCalls = new AtomicInteger(0);

        when(emailIngestionService.ingest(event)).thenReturn(true);

        doAnswer(invocation -> {
            acknowledgeCalls.incrementAndGet();
            return null;
        }).when(acknowledgment).acknowledge();

        emailRequestKafkaListener.listen(record, acknowledgment);

        assertThat(acknowledgeCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("listen — вызывает acknowledge, если ingest вернул false")
    void listen_ShouldAcknowledge_WhenIngestReturnedFalse() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        ConsumerRecord<String, EmailRequestedEvent> record =
                new ConsumerRecord<>("user-registered", 0, 0L, "key-1", event);

        AtomicInteger acknowledgeCalls = new AtomicInteger(0);

        when(emailIngestionService.ingest(event)).thenReturn(false);

        doAnswer(invocation -> {
            acknowledgeCalls.incrementAndGet();
            return null;
        }).when(acknowledgment).acknowledge();

        emailRequestKafkaListener.listen(record, acknowledgment);

        assertThat(acknowledgeCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("listen — не вызывает acknowledge, если ingest выбрасывает исключение")
    void listen_ShouldNotAcknowledge_WhenIngestThrowsException() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        ConsumerRecord<String, EmailRequestedEvent> record =
                new ConsumerRecord<>("user-registered", 0, 0L, "key-1", event);

        when(emailIngestionService.ingest(event))
                .thenThrow(new IllegalArgumentException("payload.email is blank"));

        assertThatThrownBy(() -> emailRequestKafkaListener.listen(record, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload.email is blank");
    }
}
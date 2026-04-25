package org.mushroom.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailDispatchException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SingleOutboxEmailProcessorUnitTest {

    @Mock
    private OutboxEmailRepository outboxEmailRepository;

    @Mock
    private EmailDispatcher emailDispatcher;

    private SingleOutboxEmailProcessor singleOutboxEmailProcessor;

    @BeforeEach
    void setUp() {
        singleOutboxEmailProcessor = new SingleOutboxEmailProcessor(outboxEmailRepository, emailDispatcher);
        ReflectionTestUtils.setField(singleOutboxEmailProcessor, "retryDelayMinutes", 5L);
    }

    @Test
    @DisplayName("processOne — переводит запись в SENT при успешной отправке")
    void processOne_ShouldMarkAsSent_WhenDispatchSucceeded() throws Exception {
        OutboxEmail email = buildEmail(1L, "user1@test.local:1", OutboxEmailType.WELCOME);

        singleOutboxEmailProcessor.processOne(email);

        assertThat(email.getStatus()).isEqualTo(OutboxEmailStatus.SENT);
        assertThat(email.getAttempts()).isEqualTo(0);
        assertThat(email.getUpdatedAt()).isNotNull();

        verify(outboxEmailRepository).save(email);
    }

    @Test
    @DisplayName("processOne — переводит запись в FAILED и откладывает nextAttemptAt при ошибке отправки")
    void processOne_ShouldMarkAsFailed_WhenDispatchFailed() throws Exception {
        OutboxEmail email = buildEmail(1L, "user1@test.local:1", OutboxEmailType.WELCOME);
        LocalDateTime before = LocalDateTime.now();

        doThrow(new EmailDispatchException("smtp down"))
                .when(emailDispatcher)
                .send(email);

        singleOutboxEmailProcessor.processOne(email);

        assertThat(email.getStatus()).isEqualTo(OutboxEmailStatus.FAILED);
        assertThat(email.getAttempts()).isEqualTo(1);
        assertThat(email.getNextAttemptAt()).isAfterOrEqualTo(before.plusMinutes(5));
        assertThat(email.getUpdatedAt()).isNotNull();

        verify(outboxEmailRepository).save(email);
    }

    @Test
    @DisplayName("processOne — выбрасывает OutboxEmailProcessingException при неожиданной ошибке")
    void processOne_ShouldThrow_WhenUnexpectedErrorOccurs() throws Exception {
        OutboxEmail email = buildEmail(1L, "user1@test.local:1", OutboxEmailType.WELCOME);

        doThrow(new RuntimeException("unexpected"))
                .when(emailDispatcher)
                .send(email);

        assertThatThrownBy(() -> singleOutboxEmailProcessor.processOne(email))
                .isInstanceOf(OutboxEmailProcessingException.class)
                .hasMessage("Failed to process outbox email id=1");
    }

    private OutboxEmail buildEmail(Long id, String dedupKey, OutboxEmailType type) {
        return OutboxEmail.builder()
                .id(id)
                .dedupKey(dedupKey)
                .type(type)
                .status(OutboxEmailStatus.PROCESSING)
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .updatedAt(LocalDateTime.now().minusMinutes(2))
                .build();
    }
}
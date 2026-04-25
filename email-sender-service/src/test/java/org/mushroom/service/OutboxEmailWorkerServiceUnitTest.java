package org.mushroom.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEmailWorkerServiceUnitTest {

    @Mock
    private OutboxEmailClaimService outboxEmailClaimService;

    @Mock
    private SingleOutboxEmailProcessor singleOutboxEmailProcessor;

    private ThreadPoolTaskExecutor emailProcessingExecutor;
    private OutboxEmailWorkerService outboxEmailWorkerService;

    @BeforeEach
    void setUp() {
        emailProcessingExecutor = new ThreadPoolTaskExecutor();
        emailProcessingExecutor.setCorePoolSize(2);
        emailProcessingExecutor.setMaxPoolSize(2);
        emailProcessingExecutor.setQueueCapacity(10);
        emailProcessingExecutor.setThreadNamePrefix("test-email-processing-");
        emailProcessingExecutor.initialize();

        outboxEmailWorkerService = new OutboxEmailWorkerService(
                outboxEmailClaimService,
                singleOutboxEmailProcessor,
                emailProcessingExecutor
        );
    }

    @AfterEach
    void tearDown() {
        emailProcessingExecutor.shutdown();
    }

    @Test
    @DisplayName("processBatch — обрабатывает все записи из батча")
    void processBatch_ShouldProcessAllClaimedEmails() throws Exception {
        OutboxEmail email10 = buildEmail(10L);
        OutboxEmail email11 = buildEmail(11L);
        OutboxEmail email12 = buildEmail(12L);

        when(outboxEmailClaimService.claimNextBatch())
                .thenReturn(List.of(email10, email11, email12));

        outboxEmailWorkerService.processBatch();

        verify(singleOutboxEmailProcessor).processOne(email10);
        verify(singleOutboxEmailProcessor).processOne(email11);
        verify(singleOutboxEmailProcessor).processOne(email12);
    }

    @Test
    @DisplayName("processBatch — ничего не делает если батч пустой")
    void processBatch_ShouldDoNothing_WhenBatchIsEmpty() throws Exception {
        when(outboxEmailClaimService.claimNextBatch()).thenReturn(List.of());

        outboxEmailWorkerService.processBatch();

        verifyNoInteractions(singleOutboxEmailProcessor);
    }

    @Test
    @DisplayName("processBatch — продолжает обработку остальных записей если одна запись упала")
    void processBatch_ShouldContinue_WhenOneProcessingFails() throws Exception {
        OutboxEmail email20 = buildEmail(20L);
        OutboxEmail email21 = buildEmail(21L);

        when(outboxEmailClaimService.claimNextBatch())
                .thenReturn(List.of(email20, email21));

        doAnswer(invocation -> {
            OutboxEmail email = invocation.getArgument(0, OutboxEmail.class);
            if (email.getId().equals(20L)) {
                throw new OutboxEmailProcessingException("boom");
            }
            return null;
        }).when(singleOutboxEmailProcessor).processOne(any(OutboxEmail.class));

        outboxEmailWorkerService.processBatch();

        verify(singleOutboxEmailProcessor).processOne(email20);
        verify(singleOutboxEmailProcessor).processOne(email21);
    }

    @Test
    @DisplayName("processBatch — пробрасывает OutboxClaimException если claim не удался")
    void processBatch_ShouldThrow_WhenClaimFails() throws Exception {
        when(outboxEmailClaimService.claimNextBatch())
                .thenThrow(new OutboxClaimException("Failed to claim outbox batch", new RuntimeException("db down")));

        assertThatThrownBy(() -> outboxEmailWorkerService.processBatch())
                .isInstanceOf(OutboxClaimException.class)
                .hasMessage("Failed to claim outbox batch");
    }

    private OutboxEmail buildEmail(Long id) {
        return OutboxEmail.builder()
                .id(id)
                .type(OutboxEmailType.WELCOME)
                .status(OutboxEmailStatus.PROCESSING)
                .dedupKey("user" + id + "@test.local:" + id)
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .updatedAt(LocalDateTime.now().minusMinutes(2))
                .build();
    }
}
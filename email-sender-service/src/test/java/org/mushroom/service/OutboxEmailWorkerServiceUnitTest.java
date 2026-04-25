package org.mushroom.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
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
    void processBatch_ShouldProcessAllClaimedIds() throws Exception {
        when(outboxEmailClaimService.claimNextBatch())
                .thenReturn(List.of(10L, 11L, 12L));

        outboxEmailWorkerService.processBatch();

        verify(singleOutboxEmailProcessor).processOne(10L);
        verify(singleOutboxEmailProcessor).processOne(11L);
        verify(singleOutboxEmailProcessor).processOne(12L);
    }

    @Test
    @DisplayName("processBatch — ничего не делает если батч пустой")
    void processBatch_ShouldDoNothing_WhenBatchIsEmpty() throws Exception {
        when(outboxEmailClaimService.claimNextBatch())
                .thenReturn(List.of());

        outboxEmailWorkerService.processBatch();

        verifyNoInteractions(singleOutboxEmailProcessor);
    }

    @Test
    @DisplayName("processBatch — продолжает обработку остальных записей если одна запись упала")
    void processBatch_ShouldContinue_WhenOneProcessingFails() throws Exception {
        when(outboxEmailClaimService.claimNextBatch())
                .thenReturn(List.of(20L, 21L));

        doAnswer(invocation -> {
            Long id = invocation.getArgument(0, Long.class);
            if (id.equals(20L)) {
                throw new OutboxEmailProcessingException("boom");
            }
            return null;
        }).when(singleOutboxEmailProcessor).processOne(anyLong());

        outboxEmailWorkerService.processBatch();

        verify(singleOutboxEmailProcessor).processOne(20L);
        verify(singleOutboxEmailProcessor).processOne(21L);
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
}
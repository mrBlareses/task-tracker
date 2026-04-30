package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.OutboxClaimException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEmailWorkerServiceUnitTest {

    @Mock
    private OutboxEmailClaimService outboxEmailClaimService;

    @Mock
    private AsyncOutboxEmailProcessor asyncOutboxEmailProcessor;

    @Test
    @DisplayName("processBatch — обрабатывает все записи из батча")
    void processBatch_ShouldProcessAllClaimedEmails() {
        OutboxEmail email10 = buildEmail(10L);
        OutboxEmail email11 = buildEmail(11L);
        OutboxEmail email12 = buildEmail(12L);

        OutboxEmailWorkerService outboxEmailWorkerService = new OutboxEmailWorkerService(
                outboxEmailClaimService,
                asyncOutboxEmailProcessor
        );

        when(outboxEmailClaimService.claimNextBatch())
                .thenReturn(List.of(email10, email11, email12));

        int actual = outboxEmailWorkerService.processBatch();

        assertThat(actual).isEqualTo(3);
        verify(asyncOutboxEmailProcessor).processAsync(email10);
        verify(asyncOutboxEmailProcessor).processAsync(email11);
        verify(asyncOutboxEmailProcessor).processAsync(email12);
    }

    @Test
    @DisplayName("processBatch — ничего не делает если батч пустой")
    void processBatch_ShouldDoNothing_WhenBatchIsEmpty() {
        OutboxEmailWorkerService outboxEmailWorkerService = new OutboxEmailWorkerService(
                outboxEmailClaimService,
                asyncOutboxEmailProcessor
        );

        when(outboxEmailClaimService.claimNextBatch()).thenReturn(List.of());

        int actual = outboxEmailWorkerService.processBatch();

        assertThat(actual).isZero();
        verifyNoInteractions(asyncOutboxEmailProcessor);
    }

    @Test
    @DisplayName("processBatch — пробрасывает OutboxClaimException если claim не удался")
    void processBatch_ShouldThrow_WhenClaimFails() {
        OutboxEmailWorkerService outboxEmailWorkerService = new OutboxEmailWorkerService(
                outboxEmailClaimService,
                asyncOutboxEmailProcessor
        );

        when(outboxEmailClaimService.claimNextBatch())
                .thenThrow(new OutboxClaimException("Failed to claim outbox batch", new RuntimeException("db down")));

        assertThatThrownBy(outboxEmailWorkerService::processBatch)
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
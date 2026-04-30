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
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEmailClaimServiceUnitTest {

    @Mock
    private OutboxEmailRepository outboxEmailRepository;

    private OutboxEmailClaimService outboxEmailClaimService;

    @BeforeEach
    void setUp() {
        outboxEmailClaimService = new OutboxEmailClaimService(outboxEmailRepository);
        ReflectionTestUtils.setField(outboxEmailClaimService, "batchSize", 3);
        ReflectionTestUtils.setField(outboxEmailClaimService, "maxAttempts", 5);
    }

    @Test
    @DisplayName("claimNextBatch — возвращает записи и переводит их в PROCESSING")
    void claimNextBatch_ShouldReturnEmailsAndUpdateStatus_WhenBatchExists() throws Exception {
        List<Long> ids = List.of(10L, 11L, 12L);
        List<OutboxEmail> emails = List.of(
                buildEmail(10L),
                buildEmail(11L),
                buildEmail(12L)
        );

        when(outboxEmailRepository.lockNextBatchIds(3, 5)).thenReturn(ids);
        when(outboxEmailRepository.findClaimedBatchByIds(ids)).thenReturn(emails);

        List<OutboxEmail> actual = outboxEmailClaimService.claimNextBatch();

        assertThat(actual).containsExactlyElementsOf(emails);

        verify(outboxEmailRepository).updateStatusByIds(ids, OutboxEmailStatus.PROCESSING);
        verify(outboxEmailRepository).findClaimedBatchByIds(ids);
    }

    @Test
    @DisplayName("claimNextBatch — возвращает пустой список если batch пустой")
    void claimNextBatch_ShouldReturnEmptyList_WhenNoIdsFound() throws Exception {
        when(outboxEmailRepository.lockNextBatchIds(3, 5)).thenReturn(List.of());

        List<OutboxEmail> actual = outboxEmailClaimService.claimNextBatch();

        assertThat(actual).isEmpty();

        verify(outboxEmailRepository).lockNextBatchIds(3, 5);
        verify(outboxEmailRepository, never()).updateStatusByIds(any(), any());
        verify(outboxEmailRepository, never()).findClaimedBatchByIds(any());
    }

    @Test
    @DisplayName("claimNextBatch — выбрасывает OutboxClaimException если claim не удался")
    void claimNextBatch_ShouldThrow_WhenClaimFails() {
        when(outboxEmailRepository.lockNextBatchIds(3, 5))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> outboxEmailClaimService.claimNextBatch())
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
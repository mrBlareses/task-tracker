package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.repository.OutboxEmailRepository;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEmailClaimServiceUnitTest {

    @Mock
    private OutboxEmailRepository outboxEmailRepository;

    @InjectMocks
    private OutboxEmailClaimService outboxEmailClaimService;

    @Test
    @DisplayName("claimNextBatch — возвращает список id и переводит их в PROCESSING")
    void claimNextBatch_ShouldReturnIdsAndUpdateStatus_WhenBatchExists() throws Exception {
        when(outboxEmailRepository.lockNextBatchIds(any(), eq(3)))
                .thenReturn(List.of(10L, 11L, 12L));

        List<Long> actual = outboxEmailClaimService.claimNextBatch(3);

        assertThat(actual).containsExactly(10L, 11L, 12L);

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        boolean hasUpdateCall = invocations.stream()
                .anyMatch(invocation ->
                        invocation.getMethod().getName().equals("updateStatusByIds")
                                && invocation.getArgument(1) == OutboxEmailStatus.PROCESSING
                );

        assertThat(hasUpdateCall).isTrue();
    }

    @Test
    @DisplayName("claimNextBatch — возвращает пустой список если batch пустой")
    void claimNextBatch_ShouldReturnEmptyList_WhenNoIdsFound() throws Exception {
        when(outboxEmailRepository.lockNextBatchIds(any(), eq(5)))
                .thenReturn(List.of());

        List<Long> actual = outboxEmailClaimService.claimNextBatch(5);

        assertThat(actual).isEmpty();

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        long updateCalls = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("updateStatusByIds"))
                .count();

        assertThat(updateCalls).isZero();
    }

    @Test
    @DisplayName("claimNextBatch — выбрасывает OutboxClaimException если claim не удался")
    void claimNextBatch_ShouldThrow_WhenClaimFails() {
        doThrow(new RuntimeException("db down"))
                .when(outboxEmailRepository)
                .lockNextBatchIds(any(), eq(10));

        assertThatThrownBy(() -> outboxEmailClaimService.claimNextBatch(10))
                .isInstanceOf(OutboxClaimException.class)
                .hasMessage("Failed to claim outbox batch");
    }
}
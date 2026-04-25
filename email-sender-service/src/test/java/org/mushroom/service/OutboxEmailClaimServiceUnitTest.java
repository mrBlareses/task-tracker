package org.mushroom.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.entity.enums.OutboxEmailStatus;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.repository.OutboxEmailRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

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
    @DisplayName("claimNextBatch — возвращает список id и переводит их в PROCESSING")
    void claimNextBatch_ShouldReturnIdsAndUpdateStatus_WhenBatchExists() throws Exception {
        when(outboxEmailRepository.lockNextBatchIds(3, 5))
                .thenReturn(List.of(10L, 11L, 12L));

        List<Long> actual = outboxEmailClaimService.claimNextBatch();

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
        when(outboxEmailRepository.lockNextBatchIds(3, 5))
                .thenReturn(List.of());

        List<Long> actual = outboxEmailClaimService.claimNextBatch();

        assertThat(actual).isEmpty();

        Collection<Invocation> invocations = mockingDetails(outboxEmailRepository).getInvocations();

        long updateCalls = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("updateStatusByIds"))
                .count();

        assertThat(updateCalls).isZero();
    }

    @Test
    @DisplayName("claimNextBatch — не выбирает записи с исчерпанным лимитом попыток")
    void claimNextBatch_ShouldUseConfiguredMaxAttempts() throws Exception {
        when(outboxEmailRepository.lockNextBatchIds(3, 5))
                .thenReturn(List.of(20L));

        List<Long> actual = outboxEmailClaimService.claimNextBatch();

        assertThat(actual).containsExactly(20L);
    }

    @Test
    @DisplayName("claimNextBatch — выбрасывает OutboxClaimException если claim не удался")
    void claimNextBatch_ShouldThrow_WhenClaimFails() {
        doThrow(new RuntimeException("db down"))
                .when(outboxEmailRepository)
                .lockNextBatchIds(3, 5);

        assertThatThrownBy(() -> outboxEmailClaimService.claimNextBatch())
                .isInstanceOf(OutboxClaimException.class)
                .hasMessage("Failed to claim outbox batch");
    }
}
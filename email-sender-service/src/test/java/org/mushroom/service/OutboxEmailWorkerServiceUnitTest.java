package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.exception.OutboxEmailProcessingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEmailWorkerServiceUnitTest {

    @Test
    @DisplayName("processBatch — ничего не делает если claim вернул пустой batch")
    void processBatch_ShouldDoNothing_WhenBatchIsEmpty() throws Exception {
        OutboxEmailClaimService claimService = mock(OutboxEmailClaimService.class);
        SingleOutboxEmailProcessor processor = mock(SingleOutboxEmailProcessor.class);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);

        when(claimService.claimNextBatch(50)).thenReturn(List.of());

        OutboxEmailWorkerService service =
                new OutboxEmailWorkerService(claimService, processor, executor);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchSize", 50);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "retryDelayMinutes", 5L);

        service.processBatch();

        Collection<org.mockito.invocation.Invocation> processorInvocations =
                mockingDetails(processor).getInvocations();

        assertThat(processorInvocations).isEmpty();
    }

    @Test
    @DisplayName("processBatch — обрабатывает все id из batch")
    void processBatch_ShouldProcessAllIds_WhenBatchExists() throws Exception {
        OutboxEmailClaimService claimService = mock(OutboxEmailClaimService.class);
        SingleOutboxEmailProcessor processor = mock(SingleOutboxEmailProcessor.class);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);

        when(claimService.claimNextBatch(50)).thenReturn(List.of(1L, 2L, 3L));

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        OutboxEmailWorkerService service =
                new OutboxEmailWorkerService(claimService, processor, executor);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchSize", 50);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "retryDelayMinutes", 5L);

        service.processBatch();

        Collection<org.mockito.invocation.Invocation> invocations =
                mockingDetails(processor).getInvocations();

        long processCalls = invocations.stream()
                .filter(invocation -> invocation.getMethod().getName().equals("processOne"))
                .count();

        assertThat(processCalls).isEqualTo(3);
    }

    @Test
    @DisplayName("processBatch — пробрасывает OutboxClaimException если claim не удался")
    void processBatch_ShouldThrow_WhenClaimFails() throws Exception {
        OutboxEmailClaimService claimService = mock(OutboxEmailClaimService.class);
        SingleOutboxEmailProcessor processor = mock(SingleOutboxEmailProcessor.class);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);

        when(claimService.claimNextBatch(50))
                .thenThrow(new OutboxClaimException("Failed to claim outbox batch", new RuntimeException("db down")));

        OutboxEmailWorkerService service =
                new OutboxEmailWorkerService(claimService, processor, executor);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchSize", 50);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "retryDelayMinutes", 5L);

        assertThatThrownBy(service::processBatch)
                .isInstanceOf(OutboxClaimException.class)
                .hasMessage("Failed to claim outbox batch");
    }

    @Test
    @DisplayName("processBatch — не останавливает весь batch если одна запись не обработалась")
    void processBatch_ShouldContinue_WhenSingleRecordFails() throws Exception {
        OutboxEmailClaimService claimService = mock(OutboxEmailClaimService.class);
        SingleOutboxEmailProcessor processor = mock(SingleOutboxEmailProcessor.class);
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);

        when(claimService.claimNextBatch(50)).thenReturn(List.of(1L, 2L));

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        AtomicInteger counter = new AtomicInteger();

        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            if (id.equals(1L)) {
                throw new OutboxEmailProcessingException("Failed to process outbox email id=1");
            }
            counter.incrementAndGet();
            return null;
        }).when(processor).processOne(any(Long.class), anyLong());

        OutboxEmailWorkerService service =
                new OutboxEmailWorkerService(claimService, processor, executor);

        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchSize", 50);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "retryDelayMinutes", 5L);

        service.processBatch();

        assertThat(counter.get()).isEqualTo(1);
    }
}
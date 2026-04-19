package org.mushroom.job;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.service.OutboxEmailWorkerService;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

class OutboxEmailProcessingJobUnitTest {

    @Test
    @DisplayName("processPendingEmails — делегирует вызов в worker service")
    void processPendingEmails_ShouldDelegateToWorkerService() throws Exception {
        OutboxEmailWorkerService workerService = mock(OutboxEmailWorkerService.class);

        OutboxEmailProcessingJob job = new OutboxEmailProcessingJob(workerService);

        job.processPendingEmails();

        Collection<org.mockito.invocation.Invocation> invocations =
                mockingDetails(workerService).getInvocations();

        boolean hasProcessCall = invocations.stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("processBatch"));

        assertThat(hasProcessCall).isTrue();
    }

    @Test
    @DisplayName("processPendingEmails — не выбрасывает исключение наружу если worker упал на claim")
    void processPendingEmails_ShouldNotThrow_WhenWorkerFails() throws Exception {
        OutboxEmailWorkerService workerService = mock(OutboxEmailWorkerService.class);

        doThrow(new OutboxClaimException("Failed to claim outbox batch", new RuntimeException("db down")))
                .when(workerService)
                .processBatch();

        OutboxEmailProcessingJob job = new OutboxEmailProcessingJob(workerService);

        job.processPendingEmails();

        Collection<org.mockito.invocation.Invocation> invocations =
                mockingDetails(workerService).getInvocations();

        boolean hasProcessCall = invocations.stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("processBatch"));

        assertThat(hasProcessCall).isTrue();
    }
}
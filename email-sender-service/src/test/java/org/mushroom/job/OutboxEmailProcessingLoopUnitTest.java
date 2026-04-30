package org.mushroom.job;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mushroom.exception.OutboxClaimException;
import org.mushroom.service.OutboxEmailWorkerService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEmailProcessingLoopUnitTest {

    @Mock
    private OutboxEmailWorkerService outboxEmailWorkerService;

    private OutboxEmailProcessingLoop outboxEmailProcessingLoop;

    @BeforeEach
    void setUp() {
        outboxEmailProcessingLoop = new OutboxEmailProcessingLoop(outboxEmailWorkerService);
        ReflectionTestUtils.setField(outboxEmailProcessingLoop, "idleDelayMs", 10L);
    }

    @AfterEach
    void tearDown() {
        outboxEmailProcessingLoop.stop();
    }

    @Test
    @DisplayName("start — запускает background loop и вызывает processBatch")
    void start_ShouldLaunchLoopAndInvokeProcessBatch(){
        when(outboxEmailWorkerService.processBatch()).thenReturn(0);

        outboxEmailProcessingLoop.start();

        verify(outboxEmailWorkerService, timeout(500).atLeastOnce()).processBatch();
        assertThat(outboxEmailProcessingLoop.isRunning()).isTrue();
    }

    @Test
    @DisplayName("stop — корректно останавливает loop")
    void stop_ShouldStopLoop() {
        when(outboxEmailWorkerService.processBatch()).thenReturn(0);

        outboxEmailProcessingLoop.start();
        outboxEmailProcessingLoop.stop();

        assertThat(outboxEmailProcessingLoop.isRunning()).isFalse();
    }

    @Test
    @DisplayName("loop — продолжает работу после OutboxClaimException")
    void loop_ShouldContinueAfterClaimException() {
        when(outboxEmailWorkerService.processBatch())
                .thenThrow(new OutboxClaimException("Failed to claim outbox batch", new RuntimeException("db down")));

        outboxEmailProcessingLoop.start();

        verify(outboxEmailWorkerService, timeout(500).atLeastOnce()).processBatch();
        assertThat(outboxEmailProcessingLoop.isRunning()).isTrue();
    }
}
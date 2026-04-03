package org.mushroom.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyAnalyticsSchedulerUnitTest {

    @Mock
    private DailyAnalyticsTriggerService dailyAnalyticsTriggerService;

    @InjectMocks
    private DailyAnalyticsScheduler dailyAnalyticsScheduler;

    @Test
    @DisplayName("runDailyAnalyticsJob — вызывает trigger с CRON")
    void runDailyAnalyticsJob_ShouldCallTrigger() {
        dailyAnalyticsScheduler.runDailyAnalyticsJob();

        verify(dailyAnalyticsTriggerService).trigger(TriggerType.CRON);
    }

    @Test
    @DisplayName("runDailyAnalyticsJob — не пробрасывает исключение наружу")
    void runDailyAnalyticsJob_ShouldSwallowException() {
        doThrow(new IllegalStateException("boom"))
                .when(dailyAnalyticsTriggerService)
                .trigger(TriggerType.CRON);

        dailyAnalyticsScheduler.runDailyAnalyticsJob();

        verify(dailyAnalyticsTriggerService).trigger(TriggerType.CRON);
    }
}
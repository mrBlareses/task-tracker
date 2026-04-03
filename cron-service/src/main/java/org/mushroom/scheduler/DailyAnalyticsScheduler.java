package org.mushroom.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAnalyticsScheduler {

    private final DailyAnalyticsTriggerService dailyAnalyticsTriggerService;

    @Scheduled(
            cron = "${cron.daily-analytics.expression}",
            zone = "${cron.daily-analytics.zone}"
    )
    public void runDailyAnalyticsJob() {
        log.info("Starting Daily Analytics cron job");

        try {
            dailyAnalyticsTriggerService.trigger(TriggerType.CRON);
            log.info("Daily Analytics cron job finished");
        } catch (Exception ex) {
            log.error("Failed to trigger Daily Analytics job", ex);
        }
    }
}
package org.mushroom.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DailyAnalyticsManualTriggerController {

    private final DailyAnalyticsTriggerService dailyAnalyticsTriggerService;

    @PostMapping("/api/v1/internal/cron/daily-analytics/run")
    public Mono<ResponseEntity<Void>> runDailyAnalyticsManually() {
        log.info("Received manual trigger request for daily analytics");

        return Mono.fromCallable(() -> {
                    dailyAnalyticsTriggerService.trigger(TriggerType.MANUAL);
                    return ResponseEntity.accepted().<Void>build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
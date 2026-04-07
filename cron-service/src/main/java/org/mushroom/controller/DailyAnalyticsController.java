package org.mushroom.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.enums.TriggerType;
import org.mushroom.model.JobExecutionResponse;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.mushroom.service.JobExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/cron/daily-analytics")
public class DailyAnalyticsController {

    private static final String JOB_NAME = "DAILY_ANALYTICS";

    private final DailyAnalyticsTriggerService dailyAnalyticsTriggerService;
    private final JobExecutionService jobExecutionService;

    @PostMapping("/run")
    public ResponseEntity<Void> runDailyAnalyticsManually() {
        log.info("Received manual trigger request for daily analytics");

        dailyAnalyticsTriggerService.trigger(TriggerType.MANUAL);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/executions")
    public ResponseEntity<List<JobExecutionResponse>> getRecentExecutions() {
        return ResponseEntity.ok(jobExecutionService.getRecentExecutions(JOB_NAME));
    }
}
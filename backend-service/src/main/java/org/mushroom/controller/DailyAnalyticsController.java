package org.mushroom.controller;

import lombok.RequiredArgsConstructor;
import org.mushroom.service.api.ServiceAnalyticsApi;
import org.mushroom.service.DailyAnalyticsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DailyAnalyticsController implements ServiceAnalyticsApi {

    private final DailyAnalyticsService dailyAnalyticsService;

    @Override
    public ResponseEntity<Void> collectDailyAnalytics() {
        dailyAnalyticsService.collectAndSendDailyAnalytics();
        return ResponseEntity.accepted().build();
    }
}
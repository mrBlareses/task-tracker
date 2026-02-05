package org.mushroom.controller;

import lombok.RequiredArgsConstructor;
import org.mushroom.service.api.ServiceAnalyticsApi;
import org.mushroom.service.DailyAnalyticsService;
import org.mushroom.service.model.DailyAnalyticsResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DailyAnalyticsController implements ServiceAnalyticsApi {

    private final DailyAnalyticsService dailyAnalyticsService;

    @Override
    public ResponseEntity<DailyAnalyticsResponse> getDailyAnalytics() {
        dailyAnalyticsService.getDailyAnalytics();
        return ResponseEntity.accepted().build();
    }
}

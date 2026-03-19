package org.mushroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.client.BackendTriggerClient;
import org.mushroom.service.DailyAnalyticsTriggerService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyAnalyticsTriggerServiceImpl implements DailyAnalyticsTriggerService {

    private final BackendTriggerClient backendTriggerClient;

    @Override
    public void trigger() {
        log.info("Triggering daily analytics via BackendTriggerClient");
        backendTriggerClient.triggerDailyAnalytics();
    }
}

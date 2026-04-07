package org.mushroom.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.feign.BackendTriggerFeignClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackendGateway {

    private final BackendTriggerFeignClient backendTriggerFeignClient;

    public void triggerDailyAnalytics(String accessToken) {
        log.info("Calling backend analytics endpoint");
        backendTriggerFeignClient.triggerDailyAnalytics("Bearer " + accessToken);
    }
}
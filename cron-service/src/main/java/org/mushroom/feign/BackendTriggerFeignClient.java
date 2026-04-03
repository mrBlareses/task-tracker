package org.mushroom.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "backendTriggerFeignClient", url = "${cron.daily-analytics.backend-base-url}")
public interface BackendTriggerFeignClient {

    @PostMapping("${cron.daily-analytics.trigger-path}")
    void triggerDailyAnalytics(@RequestHeader("Authorization") String authorizationHeader);
}

package org.mushroom.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "cron.daily-analytics")
public class SchedulerProperties {
    private String expression;
    private String zone;
    private String backendBaseUrl;
    private String triggerPath;
    private Duration staleThreshold;
}
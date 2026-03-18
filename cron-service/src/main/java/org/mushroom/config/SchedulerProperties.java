package org.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cron.daily-analytics")
public class SchedulerProperties {

    private String expression;

    private String zone;
}

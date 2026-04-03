package org.mushroom.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "retry.daily-analytics")
public class BackendRetryProperties {
    private int maxAttempts;
    private Duration firstBackoff;
    private Duration secondBackoff;
    private Duration thirdBackoff;
}
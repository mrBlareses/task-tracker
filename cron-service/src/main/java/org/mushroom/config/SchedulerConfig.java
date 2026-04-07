package org.mushroom.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableFeignClients(basePackages = "org.mushroom.feign")
@EnableConfigurationProperties({
        AuthProperties.class,
        SchedulerProperties.class,
        BackendRetryProperties.class
})
public class SchedulerConfig {
}
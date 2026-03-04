package org.mushroom.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.reports.UserDailyReportProjection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, KafkaEvent<?>> kafkaTemplate;

    @Value("${app.kafka.topics.daily-report}")
    private String dailyReportTopic;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    public void publishDailyAnalyticsEvent(UserDailyReportProjection event) {
        String key = event.getUserId().toString();

        kafkaTemplate.send(
                dailyReportTopic,
                key,
                new KafkaEvent<>(event)
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish daily-analytics event, key={}, userId={}",
                        key,
                        event.getUserId(),
                        ex
                );
            }
        });
    }

    public void publishUserRegistrationEvent(String email) {

        kafkaTemplate.send(
                userRegisteredTopic,
                email,
                new KafkaEvent<>(email)
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish user-registered event, key={}, email={}",
                        email,
                        email,
                        ex
                );
            }
        });
    }
}

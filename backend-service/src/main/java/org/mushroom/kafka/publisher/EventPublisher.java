package org.mushroom.kafka.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.kafka.KafkaEvent;
import org.mushroom.reports.DailyReportRow;
import org.mushroom.reports.UserRegisteredReport;
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

    public void publishDailyAnalyticsEvent(DailyReportRow event) {
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

    public void publishUserRegistrationEvent(UserRegisteredReport event) {
        String key = event.email();

        kafkaTemplate.send(
                userRegisteredTopic,
                key,
                new KafkaEvent<>(event)
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish user-registered event, key={}, email={}",
                        key,
                        event.email(),
                        ex
                );
            }
        });
    }
}

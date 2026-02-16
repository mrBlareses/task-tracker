package org.mushroom.kafka.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.kafka.KafkaEvent;
import org.mushroom.reports.DailyReportRow;
import org.mushroom.reports.UserRegisteredReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, KafkaEvent<?>> kafkaTemplate;

    @Value("${app.kafka.topics.daily-report:daily-report}")
    private String dailyReportTopic;

    @Value("${app.kafka.topics.user-registered:user-registered}")
    private String userRegisteredTopic;

    public void publishDailyAnalyticsEvent(DailyReportRow event) {
        kafkaTemplate.send(dailyReportTopic, event.getUserId().toString(), new KafkaEvent<>(event));
    }

    public void publishUserRegistrationEvent(UserRegisteredReport event) {
        String key = event.userId().toString();

        log.info("Publishing user-registered, key={}, email={}",
                key, event.email());

        kafkaTemplate.send(
                userRegisteredTopic,
                key,
                new KafkaEvent<>(event)
        );
    }
}

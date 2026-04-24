package org.mushroom.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.contract.avro.UserDailyReportEventAvro;
import org.mushroom.contract.avro.UserRegisteredEventAvro;
import org.mushroom.entity.User;
import org.mushroom.reports.UserDailyReportProjection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.daily-report}")
    private String dailyReportTopic;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    public void publishDailyAnalyticsEvent(UserDailyReportProjection projection) {
        UserDailyReportEventAvro event = UserDailyReportEventAvro.newBuilder()
                .setUserId(projection.getUserId().toString())
                .setEmail(projection.getEmail())
                .setPendingCount(projection.getPendingCount())
                .setCompletedCount(projection.getCompletedCount())
                .build();

        String key = event.getUserId();

        kafkaTemplate.send(dailyReportTopic, key, event)
                .whenComplete((result, ex) -> {
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

    public void publishUserRegistrationEvent(User user) {
        UserRegisteredEventAvro event = UserRegisteredEventAvro.newBuilder()
                .setId(user.getId().toString())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstname())
                .setLastName(user.getLastname())
                .build();

        String key = event.getEmail();

        kafkaTemplate.send(userRegisteredTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error(
                                "Failed to publish user-registered event, key={}, email={}",
                                key,
                                event.getEmail(),
                                ex
                        );
                    }
                });
    }
}
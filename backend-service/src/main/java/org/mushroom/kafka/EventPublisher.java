package org.mushroom.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.User;
import org.mushroom.model.UserDailyReportEvent;
import org.mushroom.model.UserRegisteredEvent;
import org.mushroom.reports.UserDailyReportProjection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, KafkaEvent<?>> kafkaTemplate;

    @Value("${app.kafka.topics.daily-report}")
    private String dailyReportTopic;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    public void publishDailyAnalyticsEvent(UserDailyReportProjection projection) {
        UserDailyReportEvent event = UserDailyReportEvent.builder()
                .userId(projection.getUserId().toString())
                .email(projection.getEmail())
                .pendingCount(projection.getPendingCount())
                .completedCount(projection.getCompletedCount())
                .build();

        String key = event.userId();

        kafkaTemplate.send(
                dailyReportTopic,
                key,
                new KafkaEvent<>(event)
        ).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish daily-analytics event, key={}, userId={}",
                        key,
                        event.userId(),
                        ex
                );
            }
        });
    }

    public void publishUserRegistrationEvent(User user) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .firstName(user.getFirstname())
                .lastName(user.getLastname())
                .build();

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
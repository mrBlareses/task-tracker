package org.mushroom.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailDispatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailDispatcher {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public void send(OutboxEmail email) throws EmailDispatchException {
        try {
            JsonNode payload = email.getPayload();

            String recipient = requireText(payload, "email");
            String subject = buildSubject(email.getType());
            String body = buildBody(email.getType(), payload);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info(
                    "Email dispatched successfully: id={}, type={}, to={}",
                    email.getId(),
                    email.getType(),
                    recipient
            );
        } catch (Exception ex) {
            throw new EmailDispatchException(
                    "Failed to dispatch email id=" + email.getId(),
                    ex
            );
        }
    }

    private String buildSubject(OutboxEmailType type) {
        return switch (type) {
            case WELCOME -> "Welcome to Task Tracker";
            case REPORT -> "Your daily task report";
        };
    }

    private String buildBody(OutboxEmailType type, JsonNode payload) {
        return switch (type) {
            case WELCOME -> buildWelcomeBody(payload);
            case REPORT -> buildReportBody(payload);
        };
    }

    private String buildWelcomeBody(JsonNode payload) {
        String firstName = textOrDefault(payload, "firstName", "User");
        String lastName = textOrDefault(payload, "lastName", "");
        String fullName = (firstName + " " + lastName).trim();
        String email = requireText(payload, "email");

        return """
                Hello, %s!

                Welcome to Task Tracker.

                Your account has been created successfully.
                Email: %s

                Best regards,
                Task Tracker
                """.formatted(fullName, email);
    }

    private String buildReportBody(JsonNode payload) {
        String email = requireText(payload, "email");
        int pendingCount = payload.path("pendingCount").asInt();
        int completedCount = payload.path("completedCount").asInt();

        return """
                Hello!

                Here is your daily Task Tracker report.

                Email: %s
                Pending tasks: %d
                Completed today: %d

                Best regards,
                Task Tracker
                """.formatted(email, pendingCount, completedCount);
    }

    private String requireText(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        if (node == null || node.asText().isBlank()) {
            throw new IllegalStateException("Payload field '%s' is missing or blank".formatted(fieldName));
        }
        return node.asText();
    }

    private String textOrDefault(JsonNode payload, String fieldName, String defaultValue) {
        JsonNode node = payload.get(fieldName);
        if (node == null || node.asText().isBlank()) {
            return defaultValue;
        }
        return node.asText();
    }
}
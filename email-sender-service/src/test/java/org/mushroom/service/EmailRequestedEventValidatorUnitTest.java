package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailEventValidationException;
import org.mushroom.model.EmailRequestedEvent;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailRequestedEventValidatorUnitTest {

    private final EmailRequestedEventValidator validator = new EmailRequestedEventValidator();

    @Test
    @DisplayName("validateAndBuildDedupKey — возвращает dedupKey для валидного события")
    void validateAndBuildDedupKey_ShouldReturnDedupKey_WhenEventIsValid() throws Exception {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1,
                        "username", "user1"
                ))
                .build();

        String actual = validator.validateAndBuildDedupKey(event);

        assertThat(actual).isEqualTo("user1@test.local:1");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если событие null")
    void validateAndBuildDedupKey_ShouldThrow_WhenEventIsNull() {
        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(null))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("event is null");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если type null")
    void validateAndBuildDedupKey_ShouldThrow_WhenTypeIsNull() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(null)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("type is null");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload null")
    void validateAndBuildDedupKey_ShouldThrow_WhenPayloadIsNull() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(null)
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload is empty");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload пустой")
    void validateAndBuildDedupKey_ShouldThrow_WhenPayloadIsEmpty() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of())
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload is empty");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если email отсутствует")
    void validateAndBuildDedupKey_ShouldThrow_WhenEmailIsMissing() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of("id", 1))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.email is blank");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если id отсутствует")
    void validateAndBuildDedupKey_ShouldThrow_WhenIdIsMissing() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of("email", "user1@test.local"))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(EmailEventValidationException.class)
                .hasMessage("payload.id is blank");
    }
}
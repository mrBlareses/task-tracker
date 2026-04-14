package org.mushroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mushroom.entity.enums.OutboxEmailType;
import org.mushroom.exception.EmailRequestedEventValidator;
import org.mushroom.model.EmailRequestedEvent;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailRequestedEventValidatorUnitTest {

    private final EmailRequestedEventValidator validator = new EmailRequestedEventValidator();

    @Test
    @DisplayName("validateAndBuildDedupKey — возвращает dedupKey в формате email:id для валидного события")
    void validateAndBuildDedupKey_ShouldReturnDedupKey_WhenEventIsValid() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1,
                        "username", "user1"
                ))
                .build();

        String dedupKey = validator.validateAndBuildDedupKey(event);

        assertThat(dedupKey).isEqualTo("user1@test.local:1");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если событие null")
    void validateAndBuildDedupKey_ShouldThrowException_WhenEventIsNull() {
        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("event is null");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если type null")
    void validateAndBuildDedupKey_ShouldThrowException_WhenTypeIsNull() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(null)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", 1
                ))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("type is null");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload null")
    void validateAndBuildDedupKey_ShouldThrowException_WhenPayloadIsNull() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(null)
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload is empty");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload пустой")
    void validateAndBuildDedupKey_ShouldThrowException_WhenPayloadIsEmpty() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of())
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload is empty");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload.email пустой")
    void validateAndBuildDedupKey_ShouldThrowException_WhenEmailIsBlank() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "   ",
                        "id", 1
                ))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload.email is blank");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload.id отсутствует")
    void validateAndBuildDedupKey_ShouldThrowException_WhenIdIsMissing() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local"
                ))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload.id is blank");
    }

    @Test
    @DisplayName("validateAndBuildDedupKey — выбрасывает исключение, если payload.id пустой")
    void validateAndBuildDedupKey_ShouldThrowException_WhenIdIsBlank() {
        EmailRequestedEvent event = EmailRequestedEvent.builder()
                .type(OutboxEmailType.WELCOME)
                .payload(Map.of(
                        "email", "user1@test.local",
                        "id", "   "
                ))
                .build();

        assertThatThrownBy(() -> validator.validateAndBuildDedupKey(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload.id is blank");
    }
}
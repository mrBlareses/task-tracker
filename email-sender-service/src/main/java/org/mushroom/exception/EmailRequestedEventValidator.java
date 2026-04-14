package org.mushroom.exception;

import org.mushroom.model.EmailRequestedEvent;
import org.springframework.stereotype.Component;

@Component
public class EmailRequestedEventValidator {

    public String validateAndBuildDedupKey(EmailRequestedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is null");
        }

        if (event.getType() == null) {
            throw new IllegalArgumentException("type is null");
        }

        if (event.getPayload() == null || event.getPayload().isEmpty()) {
            throw new IllegalArgumentException("payload is empty");
        }

        Object emailValue = event.getPayload().get("email");
        Object idValue = event.getPayload().get("id");

        String email = emailValue instanceof String ? ((String) emailValue).trim() : null;
        String id = idValue != null ? String.valueOf(idValue).trim() : null;

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("payload.email is blank");
        }

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("payload.id is blank");
        }

        return email + ":" + id;
    }
}
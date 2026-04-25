package org.mushroom.service;

import org.mushroom.exception.EmailEventValidationException;
import org.springframework.stereotype.Component;

@Component
public class EmailRequestedEventValidator {

    public String validateAndBuildDedupKey(EmailRequestedEvent event) throws EmailEventValidationException {
        if (event == null) {
            throw new EmailEventValidationException("event is null");
        }
        if (event.getType() == null) {
            throw new EmailEventValidationException("type is null");
        }
        if (event.getPayload() == null || event.getPayload().isEmpty()) {
            throw new EmailEventValidationException("payload is empty");
        }

        Object email = event.getPayload().get("email");
        if (email == null || email.toString().isBlank()) {
            throw new EmailEventValidationException("payload.email is blank");
        }

        Object id = event.getPayload().get("id");
        if (id == null || id.toString().isBlank()) {
            throw new EmailEventValidationException("payload.id is blank");
        }

        return email + ":" + id;
    }
}
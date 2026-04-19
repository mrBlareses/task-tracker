package org.mushroom.exception;

import org.mushroom.model.UserRegisteredEvent;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventValidator {

    public String validateAndBuildDedupKey(UserRegisteredEvent event) throws EmailEventValidationException {
        if (event == null) {
            throw new EmailEventValidationException("event is null");
        }

        if (event.email() == null || event.email().isBlank()) {
            throw new EmailEventValidationException("payload.email is blank");
        }

        if (event.id() == null || event.id().isBlank()) {
            throw new EmailEventValidationException("payload.id is blank");
        }

        return event.email() + ":" + event.id();
    }
}
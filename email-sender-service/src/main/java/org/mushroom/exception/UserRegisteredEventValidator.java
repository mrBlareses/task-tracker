package org.mushroom.exception;

import org.mushroom.contract.avro.UserRegisteredEventAvro;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventValidator {

    public String validateAndBuildDedupKey(UserRegisteredEventAvro event) throws EmailEventValidationException {
        if (event == null) {
            throw new EmailEventValidationException("event is null");
        }

        if (event.getEmail() == null || event.getEmail().isBlank()) {
            throw new EmailEventValidationException("payload.email is blank");
        }

        if (event.getId() == null || event.getId().isBlank()) {
            throw new EmailEventValidationException("payload.id is blank");
        }

        return event.getEmail() + ":" + event.getId();
    }
}
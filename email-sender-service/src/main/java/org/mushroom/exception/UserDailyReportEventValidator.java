package org.mushroom.exception;

import org.mushroom.contract.avro.UserDailyReportEventAvro;
import org.springframework.stereotype.Component;

@Component
public class UserDailyReportEventValidator {

    public String validateAndBuildDedupKey(UserDailyReportEventAvro event) throws EmailEventValidationException {
        if (event == null) {
            throw new EmailEventValidationException("event is null");
        }

        if (event.getEmail() == null || event.getEmail().isBlank()) {
            throw new EmailEventValidationException("payload.email is blank");
        }

        if (event.getUserId() == null || event.getUserId().isBlank()) {
            throw new EmailEventValidationException("payload.userId is blank");
        }

        return event.getEmail() + ":" + event.getUserId();
    }
}
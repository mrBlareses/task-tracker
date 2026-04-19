package org.mushroom.exception;

import org.mushroom.model.UserDailyReportEvent;
import org.springframework.stereotype.Component;

@Component
public class UserDailyReportEventValidator {

    public String validateAndBuildDedupKey(UserDailyReportEvent event) throws EmailEventValidationException {
        if (event == null) {
            throw new EmailEventValidationException("event is null");
        }

        if (event.email() == null || event.email().isBlank()) {
            throw new EmailEventValidationException("payload.email is blank");
        }

        if (event.userId() == null || event.userId().isBlank()) {
            throw new EmailEventValidationException("payload.userId is blank");
        }

        return event.email() + ":" + event.userId();
    }
}
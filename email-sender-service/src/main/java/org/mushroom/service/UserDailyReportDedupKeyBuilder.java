package org.mushroom.service;

import org.mushroom.contract.avro.UserDailyReportEventAvro;
import org.springframework.stereotype.Component;

@Component
public class UserDailyReportDedupKeyBuilder {

    public String build(UserDailyReportEventAvro event) {
        return event.getEmail() + ":" + event.getUserId();
    }
}
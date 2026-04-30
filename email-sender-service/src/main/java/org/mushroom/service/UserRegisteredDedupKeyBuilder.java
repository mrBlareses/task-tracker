package org.mushroom.service;

import org.mushroom.contract.avro.UserRegisteredEventAvro;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredDedupKeyBuilder {

    public String build(UserRegisteredEventAvro event) {
        return event.getEmail() + ":" + event.getId();
    }
}
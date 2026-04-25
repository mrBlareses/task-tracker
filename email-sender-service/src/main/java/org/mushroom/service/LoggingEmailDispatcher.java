package org.mushroom.service;

import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.exception.EmailDispatchException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailDispatcher implements EmailDispatcher {

    @Override
    public void send(OutboxEmail email) throws EmailDispatchException {
        log.info(
                "Email dispatched successfully: id={}, type={}",
                email.getId(),
                email.getType()
        );
    }
}
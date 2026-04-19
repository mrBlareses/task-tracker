package org.mushroom.service;

import lombok.extern.slf4j.Slf4j;
import org.mushroom.entity.OutboxEmail;
import org.mushroom.exception.EmailDispatchException;
import org.springframework.stereotype.Component;

@Slf4j
public class LoggingEmailDispatcher implements EmailDispatcher {

    @Override
    public void send(OutboxEmail email) throws EmailDispatchException {
        try {
            log.info("Email dispatched successfully: id={}, dedupKey={}, type={}",
                    email.getId(), email.getDedupKey(), email.getType());
        } catch (Exception ex) {
            throw new EmailDispatchException(
                    "Failed to dispatch email id=" + email.getId(),
                    ex
            );
        }
    }
}
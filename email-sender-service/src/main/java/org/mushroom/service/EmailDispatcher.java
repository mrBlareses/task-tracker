package org.mushroom.service;

import org.mushroom.entity.OutboxEmail;
import org.mushroom.exception.EmailDispatchException;

public interface EmailDispatcher {
    void send(OutboxEmail email) throws EmailDispatchException;
}
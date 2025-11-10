package org.mushroom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NotImplementedException extends ResponseStatusException {
    public NotImplementedException(String message) {
        super(HttpStatus.NOT_IMPLEMENTED, message);
    }
}
package org.mushroom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ForbiddenActionException extends ResponseStatusException {
    public ForbiddenActionException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}

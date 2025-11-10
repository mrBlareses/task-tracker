package org.mushroom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ConflictException extends ResponseStatusException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    public static ConflictException duplicateUser() {
        return new ConflictException("User already exists");
    }
}
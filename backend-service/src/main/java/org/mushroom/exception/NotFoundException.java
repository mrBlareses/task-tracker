package org.mushroom.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public class NotFoundException extends ResponseStatusException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
    public static NotFoundException userNotFoundByEmail(String email) {
        return new NotFoundException("User with email '" + email + "' not found");
    }

    public static NotFoundException userNotFoundById(UUID id) {
        return new NotFoundException("User with id '" + id + "' not found");
    }
}
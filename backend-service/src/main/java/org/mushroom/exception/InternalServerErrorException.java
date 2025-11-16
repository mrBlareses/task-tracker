package org.mushroom.exception;

public class InternalServerErrorException extends RuntimeException {
    public InternalServerErrorException(String message) {
        super(message);
    }

    public static InternalServerErrorException databaseError() {
        return new InternalServerErrorException("Database operation failed");
    }
}
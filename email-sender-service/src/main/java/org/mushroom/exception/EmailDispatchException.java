package org.mushroom.exception;

public class EmailDispatchException extends Exception {

    public EmailDispatchException(String message) {
        super(message);
    }

    public EmailDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
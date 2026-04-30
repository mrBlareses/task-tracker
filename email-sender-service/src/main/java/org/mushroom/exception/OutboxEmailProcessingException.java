package org.mushroom.exception;

public class OutboxEmailProcessingException extends EmailSenderException {

    public OutboxEmailProcessingException(String message) {
        super(message);
    }

    public OutboxEmailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
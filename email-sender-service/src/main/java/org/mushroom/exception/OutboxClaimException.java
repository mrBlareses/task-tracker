package org.mushroom.exception;

public class OutboxClaimException extends EmailSenderException {

    public OutboxClaimException(String message, Throwable cause) {
        super(message, cause);
    }
}
package com.authsystem.authservice.consumer.exception;

public class PermanentConsumerException extends RuntimeException {

    public PermanentConsumerException(String message) {
        super(message);
    }

    public PermanentConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}

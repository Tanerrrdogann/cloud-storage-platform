package com.authsystem.authservice.consumer.exception;

public class TemporaryConsumerException extends RuntimeException {

    public TemporaryConsumerException(String message) {
        super(message);
    }

    public TemporaryConsumerException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.UNAUTHORIZED; }
    @Override public String getErrorCode() { return "INVALID_OR_EXPIRED_TOKEN"; }
}
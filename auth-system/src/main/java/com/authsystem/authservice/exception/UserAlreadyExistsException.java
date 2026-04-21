package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BaseException {
    public UserAlreadyExistsException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.CONFLICT; }
    @Override public String getErrorCode() { return "USER_ALREADY_EXISTS"; }
}
package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {
    public AuthenticationException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.UNAUTHORIZED; }
    @Override public String getErrorCode() { return "AUTHENTICATION_FAILED"; }
}
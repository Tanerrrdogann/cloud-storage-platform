package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class AccessDeniedCustomException extends BaseException {
    public AccessDeniedCustomException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.FORBIDDEN; }
    @Override public String getErrorCode() { return "ACCESS_DENIED"; }
}
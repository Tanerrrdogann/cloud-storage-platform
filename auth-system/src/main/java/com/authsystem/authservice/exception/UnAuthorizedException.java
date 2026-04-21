package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class UnAuthorizedException extends BaseException {
    public UnAuthorizedException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.UNAUTHORIZED; }
    @Override public String getErrorCode() { return "UNAUTHORIZED_ACCESS"; }
}
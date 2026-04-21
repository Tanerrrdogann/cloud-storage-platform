package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseException {
    public InternalServerException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.INTERNAL_SERVER_ERROR; }
    @Override public String getErrorCode() { return "INTERNAL_SERVER_ERROR"; }
}
package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends BaseException {
    public TooManyRequestsException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.TOO_MANY_REQUESTS; }
    @Override public String getErrorCode() { return "RATE_LIMIT_EXCEEDED"; }
}
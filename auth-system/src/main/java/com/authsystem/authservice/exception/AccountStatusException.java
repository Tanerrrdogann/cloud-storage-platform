package com.authsystem.authservice.exception;
import org.springframework.http.HttpStatus;

public class AccountStatusException extends BaseException {
    public AccountStatusException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.FORBIDDEN; }
    @Override public String getErrorCode() { return "ACCOUNT_STATUS_ERROR"; }
}
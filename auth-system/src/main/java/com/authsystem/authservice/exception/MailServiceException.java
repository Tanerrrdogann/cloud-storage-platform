package com.authsystem.authservice.exception;

import org.springframework.http.HttpStatus;

public class MailServiceException extends BaseException {
    public MailServiceException(String message) { super(message); }

    @Override public HttpStatus getStatus() { return HttpStatus.SERVICE_UNAVAILABLE; }

    @Override public String getErrorCode() { return "MAIL_SERVICE_ERROR"; }
}
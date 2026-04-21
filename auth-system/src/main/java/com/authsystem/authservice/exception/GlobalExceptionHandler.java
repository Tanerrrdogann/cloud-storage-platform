package com.authsystem.authservice.exception;

import com.authsystem.authservice.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        log.warn("ST_LOG | TYPE:BUSINESS_ERROR | CODE:{} | MSG:{} | PATH:{} | IP:{}", 
            ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), request.getRemoteAddr());

        ErrorResponse error = new ErrorResponse(
            ex.getStatus().value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return new ResponseEntity<>(error, ex.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("ST_LOG | TYPE:SYSTEM_ERROR | MSG:{} | PATH:{} | IP:{}", 
            ex.getMessage(), request.getRequestURI(), request.getRemoteAddr(), ex);

        ErrorResponse error = new ErrorResponse(500, "Beklenmedik bir sistem hatası oluştu.", LocalDateTime.now());
        return new ResponseEntity<>(error, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
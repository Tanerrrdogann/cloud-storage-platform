package com.authsystem.storageservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class StorageRequestContextFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(StorageRequestContextFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String correlationId = resolveCorrelationId(request.getHeader(CORRELATION_ID_HEADER));

        try {
            MDC.put("service", "storage-service");
            MDC.put("correlationId", correlationId);
            MDC.put("path", request.getRequestURI());
            MDC.put("method", request.getMethod());
            MDC.put("ip", request.getRemoteAddr());
            MDC.put("action", "REQUEST_IN");

            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.put("action", "REQUEST_COMPLETE");
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(System.currentTimeMillis() - startTime));
            log.info("Storage request completed");
            MDC.clear();
        }
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId != null && !incomingCorrelationId.isBlank()) {
            return incomingCorrelationId;
        }

        return UUID.randomUUID().toString();
    }
}

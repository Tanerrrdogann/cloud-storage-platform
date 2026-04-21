package com.authsystem.api_gateway;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER));
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        String clientIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange.mutate().request(request).build())
                .doFirst(() -> putMdc(correlationId, path, method, clientIp, "REQUEST_IN"))
                .doFinally(signalType -> {
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 0;
                    long durationMs = System.currentTimeMillis() - startTime;
                    putMdc(correlationId, path, method, clientIp, "REQUEST_COMPLETE");
                    MDC.put("status", String.valueOf(statusCode));
                    MDC.put("durationMs", String.valueOf(durationMs));
                    org.slf4j.LoggerFactory.getLogger(CorrelationIdFilter.class)
                            .info("Gateway request completed");
                    MDC.clear();
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        return incomingCorrelationId != null && !incomingCorrelationId.isBlank()
                ? incomingCorrelationId
                : UUID.randomUUID().toString();
    }

    private void putMdc(String correlationId, String path, String method, String clientIp, String action) {
        MDC.put("correlationId", correlationId);
        MDC.put("path", path);
        MDC.put("method", method);
        MDC.put("ip", clientIp);
        MDC.put("action", action);
    }
}

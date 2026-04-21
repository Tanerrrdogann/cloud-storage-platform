package com.authsystem.api_gateway;

import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class GatewayAuthorizationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthorizationFilter.class);
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/v1/auth/",
            "/api/v1/swagger-ui/",
            "/api/v1/v3/api-docs/",
            "/actuator/",
            "/health",
            "/ready"
    );

    private final GatewayJwtService gatewayJwtService;

    public GatewayAuthorizationFilter(GatewayJwtService gatewayJwtService) {
        this.gatewayJwtService = gatewayJwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        if (!requiresGatewayAuthorization(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("ST_LOG | ACTION:GATEWAY_AUTH_REQUIRED | PATH:{} | IP:{}", path, clientIp(exchange));
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "JWT gerekli.");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = gatewayJwtService.parseToken(token);
        } catch (IllegalArgumentException ex) {
            log.warn("ST_LOG | ACTION:GATEWAY_INVALID_TOKEN | PATH:{} | IP:{}", path, clientIp(exchange));
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "Gecersiz veya suresi dolmus token.");
        }

        String role = claims.get("role", String.class);
        if (!isAuthorized(path, role)) {
            log.warn("ST_LOG | ACTION:GATEWAY_ACCESS_DENIED | PATH:{} | ROLE:{} | IP:{}", path, role, clientIp(exchange));
            return writeError(exchange, HttpStatus.FORBIDDEN, "Bu islem icin yetkiniz yok.");
        }

        String username = claims.getSubject();
        MDC.put("username", username);
        MDC.put("role", role);

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-Authenticated-User", username)
                .header("X-Authenticated-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(signalType -> {
                    MDC.remove("username");
                    MDC.remove("role");
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean requiresGatewayAuthorization(String path) {
        return path.startsWith("/api/v1/admin/")
                || path.startsWith("/api/v1/moderator/")
                || path.startsWith("/api/v1/tasks/")
                || isObjectsPath(path);
    }

    private boolean isAuthorized(String path, String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        if (path.startsWith("/api/v1/admin/")) {
            return "ROLE_ADMIN".equals(role);
        }
        if (path.startsWith("/api/v1/moderator/")) {
            return "ROLE_ADMIN".equals(role) || "ROLE_MODERATOR".equals(role);
        }
        if (path.startsWith("/api/v1/objects/admin/")) {
            return "ROLE_ADMIN".equals(role);
        }
        if (path.startsWith("/api/v1/tasks/")) {
            return "ROLE_ADMIN".equals(role) || "ROLE_USER".equals(role);
        }
        if (isObjectsPath(path)) {
            return "ROLE_ADMIN".equals(role) || "ROLE_USER".equals(role);
        }
        return true;
    }

    private boolean isObjectsPath(String path) {
        return path.equals("/api/v1/objects") || path.startsWith("/api/v1/objects/");
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\": " + status.value() + ", \"message\": \"" + message + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String clientIp(ServerWebExchange exchange) {
        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}

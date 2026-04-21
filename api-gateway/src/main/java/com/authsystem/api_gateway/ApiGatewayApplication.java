package com.authsystem.api_gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class ApiGatewayApplication {

    // 1. HATA ÇÖZÜMÜ: Spring'in URL'i properties'den okuyup temiz bir String yapmasını sağlıyoruz.
    @Value("${services.auth.url:http://localhost:8080}")
    private String authServiceUrl;

    @Value("${services.storage.url:http://localhost:8082}")
    private String storageServiceUrl;

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null 
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
            : "unknown"
        );
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 100);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service-route", r -> r.path("/api/v1/auth/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2), 2, true))
                                .circuitBreaker(c -> c.setName("authCircuitBreaker").setFallbackUri("forward:/fallback/auth"))
                        )
                        .uri(authServiceUrl)) // 2. HATA ÇÖZÜMÜ: Buraya artık değişkeni veriyoruz.

                .route("admin-service-route", r -> r.path("/api/v1/admin/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
                                .retry(retryConfig -> retryConfig.setRetries(3).setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2), 2, true))
                                .circuitBreaker(c -> c.setName("adminCircuitBreaker").setFallbackUri("forward:/fallback/admin"))
                        )
                        .uri(authServiceUrl))

                .route("moderator-service-route", r -> r.path("/api/v1/moderator/**")
                        .filters(f -> f
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
                                .retry(retryConfig -> retryConfig.setRetries(3).setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2), 2, true))
                                .circuitBreaker(c -> c.setName("moderatorCircuitBreaker").setFallbackUri("forward:/fallback/moderator"))
                        )
                        .uri(authServiceUrl))

                .route("storage-service-route", r -> r.path("/api/v1/objects/**", "/api/v1/objects")
                        .filters(f -> f
                                .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE)
                                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(2), 2, true))
                                .circuitBreaker(c -> c.setName("storageCircuitBreaker").setFallbackUri("forward:/fallback/storage"))
                        )
                        .uri(storageServiceUrl))

                .route("openapi-docs-route", r -> r.path("/api/v1/v3/api-docs/**").uri(authServiceUrl))
                .route("swagger-ui-route", r -> r.path("/api/v1/swagger-ui/**").uri(authServiceUrl))
                .route("auth-actuator-route", r -> r.path("/api/v1/actuator/**").uri(authServiceUrl))
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:3001");
        config.addAllowedOrigin("http://localhost:8088");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @RestController
    class FallbackController {
        @RequestMapping("/fallback/auth")
        public ResponseEntity<String> authFallback() {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"status\": 503, \"message\": \"Kimlik doğrulama servisi şu an meşgul veya kapalı.\"}");
        }
        @RequestMapping("/fallback/admin")
        public ResponseEntity<String> adminFallback() {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"status\": 503, \"message\": \"Admin servisi şu an meşgul.\"}");
        }
        @RequestMapping("/fallback/moderator")
        public ResponseEntity<String> moderatorFallback() {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"status\": 503, \"message\": \"Moderatör servisi şu an meşgul.\"}");
        }

        @RequestMapping("/fallback/storage")
        public ResponseEntity<String> storageFallback() {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"status\": 503, \"message\": \"Storage servisi şu an meşgul veya kapalı.\"}");
        }
    }
}

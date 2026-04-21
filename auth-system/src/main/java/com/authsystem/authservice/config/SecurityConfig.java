package com.authsystem.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.authsystem.authservice.exception.AuthenticationException;
import com.authsystem.authservice.logging.RequestContextFilter;
import com.authsystem.authservice.repository.UserRepository;
import com.authsystem.authservice.security.jwt.JwtAuthenticationFilter;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RequestContextFilter requestContextFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RequestContextFilter requestContextFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestContextFilter = requestContextFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .logout(logout -> logout.disable())
            .cors(cors -> cors.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\": 401, \"message\": \"Yetkisiz erişim: Lütfen giriş yapın.\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/auth/me").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/moderator/**").hasAnyRole("ADMIN", "MODERATOR")
                .anyRequest().authenticated()
            )
            // DÜZELTME BURADA: Filtre sıralamasını netleştiriyoruz
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            // requestContextFilter'ı en başa alıyoruz (MDC logları için)
            .addFilterBefore(requestContextFilter, JwtAuthenticationFilter.class) 
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // NOT: Eski corsConfigurationSource metodunu sildik çünkü artık Gateway patron.

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            com.authsystem.authservice.entity.User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:AUTH_USER_NOT_FOUND | USER:{}", username);
                        throw new AuthenticationException("Kullanıcı bulunamadı: " + username);
                    });
            return org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(user.getRole().name())
                    .disabled(!user.isEnabled())
                    .build();
        };
    }
}
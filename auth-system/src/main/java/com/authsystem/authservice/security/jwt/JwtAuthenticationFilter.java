package com.authsystem.authservice.security.jwt;

import com.authsystem.authservice.security.FingerprintService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FingerprintService fingerprintService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        if (authHeader != null && !authHeader.startsWith("Bearer ")) {
            log.warn("ST_LOG | TYPE:SECURITY_ALERT | ACTION:INVALID_HEADER_FORMAT | IP:{} | PATH:{}", ip, path);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Geçersiz Authorization header formatı.");
            return;
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = jwtService.extractToken(authHeader);

            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);
                String tokenFp = jwtService.extractFingerprint(token);
                String requestFp = fingerprintService.generateFingerprint(request);

                if (tokenFp.equals(requestFp)) {
                    String role = jwtService.extractRole(token);
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                    MDC.put("username", username);
                    MDC.put("role", role);
                    
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, List.of(authority));
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.info("ST_LOG | ACTION:AUTH_SUCCESS | USER:{} | IP:{}", username, ip);
                } else {
                    log.error("ST_LOG | TYPE:SECURITY_CRITICAL | ACTION:FINGERPRINT_MISMATCH | USER:{} | IP:{}", username, ip);
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Güvenlik İhlali: Cihaz uyuşmazlığı.");
                    return;
                }
            } else {
                log.warn("ST_LOG | TYPE:SECURITY_ALERT | ACTION:INVALID_TOKEN | IP:{} | PATH:{}", ip, path);
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz veya süresi dolmuş token.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format("{\"status\": %d, \"message\": \"%s\", \"timestamp\": \"%s\"}", 
                                    status, message, java.time.LocalDateTime.now());
        response.getWriter().write(json);
    }
}
package com.authsystem.authservice.security.jwt;

import com.authsystem.authservice.entity.User;
import com.authsystem.authservice.exception.InternalServerException;
import com.authsystem.authservice.exception.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET;
    
    private final long EXPIRATION = 86400000;

    @PostConstruct
    public void init() {
        if (SECRET == null || SECRET.getBytes().length < 32) {
            log.error("ST_LOG | TYPE:SYSTEM_CRITICAL | ACTION:JWT_INIT_FAILED | REASON:WEAK_SECRET");
            throw new InternalServerException("JWT secret anahtarı çok zayıf veya eksik. En az 32 byte olmalıdır.");
        }
        log.info("ST_LOG | ACTION:JWT_SERVICE_INITIALIZED | STATUS:SUCCESS");
    }

    public Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(User user, String fingerprint) {
        log.debug("ST_LOG | ACTION:TOKEN_GENERATION_START | USER:{}", user.getUsername());
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole().name())
                .claim("fp", fingerprint)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractFingerprint(String token) {
        return parseClaims(token).get("fp", String.class);
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("ST_LOG | TYPE:SECURITY_ALERT | ACTION:TOKEN_PARSE_FAILED | REASON:EXPIRED");
            throw new InvalidTokenException("Token süresi dolmuş.");
        } catch (UnsupportedJwtException | MalformedJwtException | io.jsonwebtoken.security.SignatureException e) {
            log.error("ST_LOG | TYPE:SECURITY_ALERT | ACTION:TOKEN_PARSE_FAILED | REASON:INVALID_STRUCTURE | MSG:{}", e.getMessage());
            throw new InvalidTokenException("Token yapısı veya imzası geçersiz.");
        } catch (Exception e) {
            log.error("ST_LOG | TYPE:SECURITY_ERROR | ACTION:TOKEN_PARSE_FAILED | REASON:UNKNOWN");
            throw new InvalidTokenException("Token işlenirken hata oluştu.");
        }
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("ST_LOG | TYPE:SECURITY_ALERT | ACTION:TOKEN_VALIDATION_FAILED | MSG:{}", e.getMessage());
            return false;
        }
    }

    public String extractToken(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
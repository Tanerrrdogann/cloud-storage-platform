package com.authsystem.authservice.security;

import com.authsystem.authservice.exception.InternalServerException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class FingerprintService {

    public String generateFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();

        if (userAgent == null || userAgent.isBlank()) {
            log.warn("ST_LOG | ACTION:FINGERPRINT_GENERATION | STATUS:MISSING_USER_AGENT | IP:{}", ip);
            userAgent = "unknown";
        }

        String fingerprint = sha256(userAgent);
        log.debug("ST_LOG | ACTION:FINGERPRINT_GENERATED | IP:{} | FP_HASH:{}", ip, fingerprint);
        
        return fingerprint;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();
        } catch (Exception e) {
            log.error("ST_LOG | TYPE:SYSTEM_ERROR | ACTION:SHA256_HASH_FAILED | MSG:{}", e.getMessage());
            throw new InternalServerException("Cihaz imzası oluşturulurken teknik bir hata oluştu.");
        }
    }
}
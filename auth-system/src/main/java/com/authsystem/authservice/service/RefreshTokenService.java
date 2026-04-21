package com.authsystem.authservice.service;

import com.authsystem.authservice.dto.RefreshResponse;
import com.authsystem.authservice.entity.RefreshToken;
import com.authsystem.authservice.entity.User;
import com.authsystem.authservice.exception.InvalidTokenException;
import com.authsystem.authservice.repository.RefreshTokenRepository;
import com.authsystem.authservice.security.FingerprintService;
import com.authsystem.authservice.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final FingerprintService fingerprintService;

    public RefreshToken createRefreshToken(User user, String ipAddress) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setUsed(false);
        refreshToken.setRevoked(false);

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        
        log.info("ST_LOG | ACTION:REFRESH_TOKEN_CREATED | USER:{} | IP:{}", user.getEmail(), ipAddress);
        return savedToken;
    }

    @Transactional
    public RefreshResponse refreshTokenRotate(String refreshToken, String ipAddress, HttpServletRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("ST_LOG | ACTION:TOKEN_ROTATE_FAILED | REASON:NOT_FOUND | IP:{}", ipAddress);
                    return new InvalidTokenException("Refresh token bulunamadı.");
                });

        if (token.isUsed() || token.isRevoked()) {
            log.error("ST_LOG | TYPE:SECURITY_CRITICAL | ACTION:TOKEN_REUSE_DETECTED | USER:{} | IP:{}", 
                      token.getUser().getEmail(), ipAddress);
            throw new InvalidTokenException("Güvenlik ihlali tespit edildi: Geçersiz token kullanımı.");
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("ST_LOG | ACTION:TOKEN_ROTATE_FAILED | REASON:EXPIRED | USER:{} | IP:{}", 
                     token.getUser().getEmail(), ipAddress);
            throw new InvalidTokenException("Refresh token süresi dolmuş. Lütfen tekrar giriş yapın.");
        }

        token.setUsed(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        String fingerprint = fingerprintService.generateFingerprint(request);
        String newAccessToken = jwtService.generateToken(user, fingerprint);
        
        RefreshToken newRefresh = createRefreshToken(user, ipAddress);

        log.info("ST_LOG | ACTION:AUTH_TOKEN_REFRESHED | USER:{} | IP:{}", user.getEmail(), ipAddress);
        
        return new RefreshResponse(newAccessToken, newRefresh.getToken());
    }
}
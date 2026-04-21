package com.authsystem.authservice.service;

import com.authsystem.authservice.entity.User;
import com.authsystem.authservice.exception.InternalServerException; // 🟢 Ekledik
import com.authsystem.authservice.repository.RefreshTokenRepository;
import com.authsystem.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCleanupService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteUnverifiedAccount() {
        try {
            LocalDateTime limit = LocalDateTime.now().minusMinutes(30);
            List<User> expiredUsers = userRepository.findByEnabledFalseAndVerificationTokenExpiryBefore(limit);

            if (!expiredUsers.isEmpty()) {
                log.info("ST_LOG | ACTION:CLEANUP_STARTED | TARGET_COUNT:{}", expiredUsers.size());
                
                for (User user : expiredUsers) {
                    try {
                        refreshTokenRepository.deleteByUser(user);
                        userRepository.delete(user);
                        log.info("ST_LOG | ACTION:SYSTEM_CLEANUP_DELETED_UNVERIFIED | USER_EMAIL:{}", user.getEmail());
                    } catch (Exception e) {
                        log.error("ST_LOG | TYPE:SYSTEM_ERROR | ACTION:CLEANUP_USER_FAILED | USER:{} | MSG:{}", 
                                  user.getEmail(), e.getMessage());
                    }
                }
                log.info("ST_LOG | ACTION:CLEANUP_FINISHED | STATUS:SUCCESS");
            }
        } catch (Exception e) {
            log.error("ST_LOG | TYPE:SYSTEM_CRITICAL | ACTION:ACCOUNT_CLEANUP_TASK_FAILED | MSG:{}", e.getMessage());
            throw new InternalServerException("Hesap temizleme görevi sırasında kritik hata: " + e.getMessage());
        }
    }
}
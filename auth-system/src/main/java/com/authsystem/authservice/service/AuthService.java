package com.authsystem.authservice.service;

import com.authsystem.authservice.dto.*;
import com.authsystem.authservice.entity.*;
import com.authsystem.authservice.enums.Role;
import com.authsystem.authservice.event.EventFactory;
import com.authsystem.authservice.exception.*;
import com.authsystem.authservice.mail.MailService;
import com.authsystem.authservice.repository.*;
import com.authsystem.authservice.security.FingerprintService;
import com.authsystem.authservice.security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FingerprintService fingerPrintService;
    private final RabbitMQProducer rabbitMQProducer;
    private final EventFactory eventFactory;
    private final ProcessedMessageRepository processedMessageRepository;

    @Value("${frontend.base-url:http://localhost:3001}")
    private String frontendBaseUrl;

        @Transactional
        public String register(RegisterRequest request, String ip) {
                if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("ST_LOG | ACTION:REGISTER_FAILED | REASON:USERNAME_EXISTS | USER:{} | IP:{}", request.getUsername(), ip);
                throw new UserAlreadyExistsException("Kullanıcı adı zaten kullanılıyor.");
                }
                if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("ST_LOG | ACTION:REGISTER_FAILED | REASON:EMAIL_EXISTS | EMAIL:{} | IP:{}", request.getEmail(), ip);
                throw new UserAlreadyExistsException("Email zaten kullanılıyor.");
                }

                String verifyToken = UUID.randomUUID().toString();
                User user = new User();
                user.setUsername(request.getUsername());
                user.setEmail(request.getEmail());
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user.setCreatedAt(LocalDateTime.now());
                user.setVerificationToken(verifyToken);
                user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(30));
                user.setEnabled(false);
                user.setRole(Role.ROLE_USER);

                userRepository.save(user);

                rabbitMQProducer.sendUserCreatedMessage(eventFactory.userCreated(user));

                log.info("ST_LOG | ACTION:REGISTER_SUCCESS | USER:{} | IP:{}", user.getEmail(), ip);
                mailService.sendVerificationEmail(user.getEmail(), frontendBaseUrl + "/verify?token=" + verifyToken);

                return "Kullanıcı başarıyla oluşturuldu. Lütfen mailinizi doğrulayın.";
        }

        @Transactional
        public LoginResponse login(LoginRequest request, String ip, HttpServletRequest httpRequest) {
                User user = userRepository.findByUsername(request.getUsernameOrEmail())
                        .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:LOGIN_FAILED | REASON:USER_NOT_FOUND | INPUT:{} | IP:{}", request.getUsernameOrEmail(), ip);
                        return new AuthenticationException("Geçersiz kullanıcı adı veya şifre.");
                        });

                if (user.isDeleted()) {
                log.warn("ST_LOG | ACTION:LOGIN_FAILED | REASON:ACCOUNT_DELETED | USER:{} | IP:{}", user.getUsername(), ip);
                throw new AccountStatusException("Bu hesap kalıcı olarak silinmiştir.");
                }

                if (user.getSuspensionEndTime() != null && user.getSuspensionEndTime().isAfter(LocalDateTime.now())) {
                log.warn("ST_LOG | ACTION:LOGIN_FAILED | REASON:ACCOUNT_SUSPENDED | USER:{} | IP:{}", user.getUsername(), ip);
                throw new AccountStatusException("Hesabınız geçici olarak askıya alınmıştır.");
                }

                if (isAccountLocked(user)) {
                log.warn("ST_LOG | ACTION:LOGIN_FAILED | REASON:BRUTE_FORCE_LOCK | USER:{} | IP:{}", user.getUsername(), ip);
                throw new AccountStatusException("Çok fazla hatalı giriş denemesi. 15 dakika bekleyin.");
                }

                if (!user.isEnabled()) {
                log.warn("ST_LOG | ACTION:LOGIN_FAILED | REASON:NOT_ENABLED | USER:{} | IP:{}", user.getUsername(), ip);
                throw new AccountStatusException("Hesabınız henüz doğrulanmamış.");
                }

                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                increaseFailedAttempts(user, ip);
                log.warn("ST_LOG | ACTION:LOGIN_FAILED | REASON:WRONG_PASSWORD | USER:{} | IP:{}", user.getUsername(), ip);
                throw new AuthenticationException("Geçersiz kullanıcı adı veya şifre.");
                }

                resetFailedAttempts(user);
                String fingerprint = fingerPrintService.generateFingerprint(httpRequest);
                String accessToken = jwtService.generateToken(user, fingerprint);
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, ip);

                user.setLastLoginIp(ip);
                user.setLastLoginTime(LocalDateTime.now());
                userRepository.save(user);

                rabbitMQProducer.sendUserLoggedInMessage(eventFactory.userLoggedIn(user, ip));

                log.info("ST_LOG | ACTION:LOGIN_SUCCESS | USER:{} | IP:{}", user.getEmail(), ip);
                return new LoginResponse(accessToken, refreshToken.getToken(), user.getRole());
        }

        @Transactional
        public void logout(String refreshToken, String ip) {
                RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:LOGOUT_FAILED | REASON:INVALID_TOKEN | IP:{}", ip);
                        return new InvalidTokenException("Geçersiz token.");
                        });

                token.setRevoked(true);
                refreshTokenRepository.save(token);
                SecurityContextHolder.clearContext();

                rabbitMQProducer.sendUserLoggedOutMessage(eventFactory.userLoggedOut(token.getUser()));

                log.info("ST_LOG | ACTION:LOGOUT_SUCCESS | USER:{} | IP:{}", token.getUser().getEmail(), ip);
        }

        @Transactional
        public String forgotPassword(String email, String ip) {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:FORGOT_PASSWORD_FAILED | REASON:USER_NOT_FOUND | EMAIL:{} | IP:{}", email, ip);
                        // Güvenlik gereği "Kullanıcı bulunamadı" yerine genel bir hata fırlatılabilir 
                        // ama senin akışına sadık kalarak AuthenticationException fırlatıyoruz.
                        return new AuthenticationException("Bu e-posta adresiyle kayıtlı bir kullanıcı bulunamadı.");
                        });

                String token = UUID.randomUUID().toString();
                user.setResetToken(token);
                user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
                userRepository.save(user);

                rabbitMQProducer.sendForgotPasswordMessage(eventFactory.forgotPassword(user, token));

                String resetLink = frontendBaseUrl + "/reset-password?token=" + token;
                mailService.sendResetPasswordEmail(user.getEmail(), resetLink);

                log.info("ST_LOG | ACTION:FORGOT_PASSWORD_INITIATED | USER:{} | IP:{}", user.getEmail(), ip);
                return "Şifre sıfırlama maili başarıyla gönderildi.";
        }

        @Transactional
        public String resetPassword(String token, String newPassword, String ip) {
                User user = userRepository.findByResetToken(token)
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:RESET_PASSWORD_FAILED | REASON:INVALID_TOKEN | IP:{}", ip);
                        return new InvalidTokenException("Geçersiz veya hatalı şifre sıfırlama tokenı.");
                        });

                if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
                log.warn("ST_LOG | ACTION:RESET_PASSWORD_FAILED | REASON:TOKEN_EXPIRED | USER:{} | IP:{}", user.getEmail(), ip);
                throw new InvalidTokenException("Şifre sıfırlama tokenının süresi dolmuş.");
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);

                rabbitMQProducer.sendPasswordResetMessage(eventFactory.passwordReset(user));

                log.info("ST_LOG | ACTION:RESET_PASSWORD_SUCCESS | USER:{} | IP:{}", user.getEmail(), ip);
                return "Şifreniz başarıyla değiştirildi. Yeni şifrenizle giriş yapabilirsiniz.";
        }

        @Transactional
        public String verifyUser(String token, String ip) {
                String cleanToken = token.trim();
                User user = userRepository.findByVerificationToken(cleanToken)
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:VERIFY_FAILED | REASON:INVALID_TOKEN | IP:{}", ip);
                        return new InvalidTokenException("Geçersiz doğrulama tokenı.");
                        });

                if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
                log.warn("ST_LOG | ACTION:VERIFY_FAILED | REASON:TOKEN_EXPIRED | USER:{} | IP:{}", user.getEmail(), ip);
                throw new InvalidTokenException("Doğrulama tokenının süresi dolmuş. Lütfen tekrar kayıt olun.");
                }

                user.setEnabled(true);
                user.setVerificationToken(null);
                user.setVerificationTokenExpiry(null);
                userRepository.save(user);

                rabbitMQProducer.sendUserVerifiedMessage(eventFactory.userVerified(user));

                log.info("ST_LOG | ACTION:VERIFY_SUCCESS | USER:{} | IP:{}", user.getEmail(), ip);
                return "Hesabınız başarıyla aktif edildi. Giriş yapabilirsiniz.";
        }

        private boolean isAccountLocked(User user) {
                if (user.getLockTime() == null) return false;
                if (LocalDateTime.now().isAfter(user.getLockTime().plusMinutes(15))) {
                resetFailedAttempts(user);
                return false;
                }
                return true;
        }

        private void increaseFailedAttempts(User user, String ip) {
                int newAttempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(newAttempts);
                if (newAttempts >= 5) {
                        user.setLockTime(LocalDateTime.now());
                        rabbitMQProducer.sendUserLockedMessage(eventFactory.userLocked(user, ip));
                        log.warn("ST_LOG | TYPE:SECURITY_ALERT | ACTION:ACCOUNT_LOCKED_BRUTEFORCE | USER:{} | IP:{}", user.getEmail(), ip);
                }
                userRepository.save(user);
        }

        private void resetFailedAttempts(User user) {
                user.setFailedLoginAttempts(0);
                user.setLockTime(null);
                userRepository.save(user);
        }

        @Transactional
        public String suspenUser(Long userId, int days, String actorInfo, String ipAddress) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:SUSPEND_FAILED | REASON:TARGET_NOT_FOUND | TARGET_ID:{} | IP:{}", userId, ipAddress);
                        return new AuthenticationException("Hata: Askıya alınacak kullanıcı (ID: " + userId + ") veritabanında bulunamadı.");
                        });

                User actor = userRepository.findByEmail(actorInfo)
                        .or(() -> userRepository.findByUsername(actorInfo))
                        .orElseThrow(() -> {
                        log.error("ST_LOG | ACTION:SUSPEND_FAILED | REASON:ACTOR_NOT_FOUND | ACTOR:{} | IP:{}", actorInfo, ipAddress);
                        return new AuthenticationException("Hata: İşlemi yapan yetkili (" + actorInfo + ") bulunamadı.");
                        });

                if (user.getRole() == Role.ROLE_ADMIN) {
                log.error("ST_LOG | TYPE:SECURITY_ALERT | ACTION:SUSPEND_DENIED | REASON:TARGET_IS_ADMIN | ACTOR:{} | IP:{}", actorInfo, ipAddress);
                throw new AccountStatusException("Hata: Admin hesabı askıya alınamaz.");
                }

                if (actor.getRole() == Role.ROLE_MODERATOR && user.getRole() != Role.ROLE_USER) {
                log.warn("ST_LOG | ACTION:SUSPEND_DENIED | REASON:INSUFFICIENT_MOD_POWER | ACTOR:{} | IP:{}", actorInfo, ipAddress);
                throw new AccountStatusException("Hata: Moderator sadece normal kullanıcıları engelleyebilir.");
                }

                if (days == 0) {
                if (user.getSuspensionEndTime() == null || user.getSuspensionEndTime().isBefore(LocalDateTime.now())) {
                        log.info("ST_LOG | ACTION:UNSUSPEND_IGNORED | REASON:NOT_SUSPENDED | TARGET:{} | IP:{}", user.getUsername(), ipAddress);
                        return "Kullanıcı zaten engelli değil.";
                }
                
                user.setSuspensionEndTime(null);
                userRepository.save(user);
                
                log.info("ST_LOG | ACTION:UNSUSPEND_SUCCESS | ADMIN:{} | TARGET:{} | IP:{}", actorInfo, user.getUsername(), ipAddress);
                return "Kullanıcı engeli kaldırıldı";
                }

                if (days > 5) {
                log.warn("ST_LOG | ACTION:SUSPEND_FAILED | REASON:EXCEEDED_MAX_DAYS | ACTOR:{} | DAYS:{}", actorInfo, days);
                throw new AccountStatusException("Hata: Bir kullanıcıyı tek seferde en fazla 5 gün askıya alabilirsiniz.");
                }

                user.setSuspensionEndTime(LocalDateTime.now().plusDays(days));
                userRepository.save(user);

                rabbitMQProducer.sendUserSuspendedMessage(eventFactory.userSuspended(user, days, actorInfo));

                log.info("ST_LOG | ACTION:SUSPEND_SUCCESS | ADMIN:{} | TARGET:{} | DAYS:{} | IP:{}", actorInfo, user.getUsername(), days, ipAddress);
                return "Kullanıcı engellendi";
        }

        @Transactional
        public String permanentDelete(Long userId, String actorEmail, String ip) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new AuthenticationException("Kullanıcı bulunamadı."));

                if (user.getRole() == Role.ROLE_ADMIN) {
                log.error("ST_LOG | TYPE:SECURITY_CRITICAL | ACTION:DELETE_ADMIN_DENIED | ACTOR:{} | IP:{}", actorEmail, ip);
                throw new AccountStatusException("Bir Admin hesabı silinemez.");
                }

                user.setDeleted(true);
                user.setEnabled(false);
                user.setUsername("deleted_" + UUID.randomUUID().toString().substring(0, 8));
                userRepository.save(user);

                rabbitMQProducer.sendUserDeletedMessage(eventFactory.userDeleted(user));
                
                log.info("ST_LOG | ACTION:ACCOUNT_DELETE_SUCCESS | ADMIN:{} | TARGET_ID:{} | IP:{}", actorEmail, userId, ip);
                return "Hesap kalıcı olarak kapatıldı.";
        }

        @Transactional
        public String uptadeUserRole(Long userId, Role newRole, String adminEmail, String ip) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new AuthenticationException("Kullanıcı bulunamadı."));

                if (user.getRole() == Role.ROLE_ADMIN) {
                throw new AccountStatusException("Admin rolü değiştirilemez.");
                }

                Role oldRole = user.getRole();
                user.setRole(newRole);
                userRepository.save(user);

                rabbitMQProducer.sendRoleUpdatedMessage(eventFactory.userRoleUpdated(user, newRole.name()));

                log.info("ST_LOG | ACTION:ROLE_CHANGE_SUCCESS | ADMIN:{} | TARGET:{} | FROM:{} | TO:{} | IP:{}", 
                        adminEmail, user.getUsername(), oldRole, newRole, ip);
                return "Kullanıcı rolü başarıyla " + newRole + " olarak değiştirildi.";
        }

        public List<UserResponse> getAllUsers() {
                log.info("ST_LOG | ACTION:FETCH_ALL_USERS | STATUS:START");
                
                return userRepository.findAll().stream()
                        .filter(user -> !user.isDeleted())
                        .map(user -> new UserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.getRole(),
                                user.isEnabled(),
                                user.getCreatedAt(),
                                user.getLastLoginIp()
                        ))
                        .collect(Collectors.toList());
        }

        public User getUserProfile(String username) {
                return userRepository.findByUsername(username)
                        .orElseThrow(() -> {
                        log.warn("ST_LOG | ACTION:FETCH_PROFILE_FAILED | REASON:USER_NOT_FOUND | USER:{}", username);
                        return new AuthenticationException("Kullanıcı profil bilgisi bulunamadı.");
                        });
        }

        @Transactional(readOnly = true)
        public AsyncEngineSummaryResponse getAsyncEngineSummary() {
                List<ProcessedMessage> messages = processedMessageRepository.findAll();

                Map<String, Long> byConsumer = messages.stream()
                        .collect(Collectors.groupingBy(ProcessedMessage::getConsumerName, Collectors.counting()));

                Map<String, Long> byEventType = messages.stream()
                        .collect(Collectors.groupingBy(message -> resolveEventType(message.getIdempotencyKey()), Collectors.counting()));

                List<ProcessedMessageResponse> recentMessages = messages.stream()
                        .sorted(Comparator.comparing(ProcessedMessage::getProcessedAt).reversed())
                        .limit(20)
                        .map(message -> new ProcessedMessageResponse(
                                message.getId(),
                                message.getIdempotencyKey(),
                                message.getEventId(),
                                message.getProcessedAt(),
                                message.getConsumerName(),
                                message.getStatus()
                        ))
                        .toList();

                return new AsyncEngineSummaryResponse(
                        messages.size(),
                        countByStatus(messages, "PROCESSED"),
                        countByStatus(messages, "FAILED"),
                        countByStatus(messages, "PROCESSING"),
                        true,
                        byConsumer.entrySet().stream()
                                .map(entry -> new AsyncStatusCount(entry.getKey(), entry.getValue()))
                                .sorted(Comparator.comparing(AsyncStatusCount::count).reversed())
                                .toList(),
                        byEventType.entrySet().stream()
                                .map(entry -> new AsyncStatusCount(entry.getKey(), entry.getValue()))
                                .sorted(Comparator.comparing(AsyncStatusCount::count).reversed())
                                .toList(),
                        recentMessages
                );
        }

        @Transactional(readOnly = true)
        public ModeratorActivitySummaryResponse getModeratorActivitySummary() {
                LocalDateTime now = LocalDateTime.now();
                List<User> users = userRepository.findAll().stream()
                        .filter(user -> !user.isDeleted())
                        .toList();

                List<UserResponse> recentUsers = users.stream()
                        .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                        .limit(10)
                        .map(this::toUserResponse)
                        .toList();

                return new ModeratorActivitySummaryResponse(
                        users.size(),
                        users.stream().filter(User::isEnabled).count(),
                        users.stream()
                                .filter(user -> user.getSuspensionEndTime() != null && user.getSuspensionEndTime().isAfter(now))
                                .count(),
                        users.stream()
                                .filter(user -> user.getLockTime() != null && user.getLockTime().plusMinutes(15).isAfter(now))
                                .count(),
                        users.stream().filter(user -> user.getRole() == Role.ROLE_MODERATOR).count(),
                        recentUsers
                );
        }

        private long countByStatus(List<ProcessedMessage> messages, String status) {
                return messages.stream()
                        .filter(message -> status.equals(message.getStatus()))
                        .count();
        }

        private String resolveEventType(String idempotencyKey) {
                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return "unknown";
                }

                int separatorIndex = idempotencyKey.indexOf(':');
                if (separatorIndex <= 0) {
                        return idempotencyKey;
                }

                return idempotencyKey.substring(0, separatorIndex);
        }

        private UserResponse toUserResponse(User user) {
                return new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        user.isEnabled(),
                        user.getCreatedAt(),
                        user.getLastLoginIp()
                );
        }
}

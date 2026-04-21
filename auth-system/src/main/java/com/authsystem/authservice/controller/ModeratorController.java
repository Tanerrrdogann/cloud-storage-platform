package com.authsystem.authservice.controller;

import com.authsystem.authservice.dto.ModeratorActivitySummaryResponse;
import com.authsystem.authservice.dto.UserResponse;
import com.authsystem.authservice.exception.TooManyRequestsException;
import com.authsystem.authservice.security.RateLimitService;
import com.authsystem.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/moderator")
public class ModeratorController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public ModeratorController(AuthService authService, RateLimitService rateLimitService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    private void checkRateLimit(HttpServletRequest request, String actionType) {
        String ip = request.getRemoteAddr();
        if (!rateLimitService.resolveBucket(ip, actionType).tryConsume(1)) {
            log.warn("ST_LOG | TYPE:RATE_LIMIT_EXCEEDED | ACTION:{} | IP:{}", actionType, ip);
            throw new TooManyRequestsException("Bu işlem için çok fazla istek gönderildi. Lütfen bekleyin.");
        }
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/suspend")
    public ResponseEntity<String> suspendUser(@RequestParam Long userId, @RequestParam int days, HttpServletRequest request) {
        checkRateLimit(request, "MOD_ACTION");

        String modEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("ST_LOG | ACTION:MOD_SUSPEND_START | MOD:{} | TARGET_ID:{} | DAYS:{} | IP:{}", 
                 modEmail, userId, days, request.getRemoteAddr());

        String result = authService.suspenUser(userId, days, modEmail, request.getRemoteAddr());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<List<UserResponse>> moderatorDashboard(HttpServletRequest request) {
        checkRateLimit(request, "MOD_DASHBOARD");

        List<UserResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/activity-summary")
    public ResponseEntity<ModeratorActivitySummaryResponse> activitySummary(HttpServletRequest request) {
        checkRateLimit(request, "MOD_ACTIVITY_SUMMARY");

        return ResponseEntity.ok(authService.getModeratorActivitySummary());
    }
}

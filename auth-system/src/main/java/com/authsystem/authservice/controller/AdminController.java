package com.authsystem.authservice.controller;

import com.authsystem.authservice.dto.AsyncEngineSummaryResponse;
import com.authsystem.authservice.dto.UserResponse;
import com.authsystem.authservice.enums.Role;
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
@RequestMapping("/admin")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ResponseEntity<List<UserResponse>> adminDashboard() {
        List<UserResponse> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/async-summary")
    public ResponseEntity<AsyncEngineSummaryResponse> asyncSummary() {
        return ResponseEntity.ok(authService.getAsyncEngineSummary());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/permanent-delete")
    public ResponseEntity<String> deleteUser(@RequestParam Long userId, HttpServletRequest request) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("ST_LOG | ACTION:ADMIN_DELETE_START | ADMIN:{} | TARGET_USER_ID:{} | IP:{}", adminEmail, userId, request.getRemoteAddr());
        
        String result = authService.permanentDelete(userId, adminEmail, request.getRemoteAddr());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/suspend")
    public ResponseEntity<String> suspendUser(@RequestParam Long userId, @RequestParam int days, HttpServletRequest request) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("ST_LOG | ACTION:ADMIN_SUSPEND_START | ADMIN:{} | TARGET_USER_ID:{} | DAYS:{} | IP:{}", adminEmail, userId, days, request.getRemoteAddr());
        
        String result = authService.suspenUser(userId, days, adminEmail, request.getRemoteAddr());
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/change-role")
    public ResponseEntity<String> changeRole(@RequestParam Long userId, @RequestParam Role newRole, HttpServletRequest request) {
        String adminEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("ST_LOG | ACTION:ADMIN_ROLE_CHANGE_START | ADMIN:{} | TARGET_USER_ID:{} | NEW_ROLE:{} | IP:{}", adminEmail, userId, newRole, request.getRemoteAddr());
        
        String result = authService.uptadeUserRole(userId, newRole, adminEmail, request.getRemoteAddr());
        return ResponseEntity.ok(result);
    }
}

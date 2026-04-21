package com.authsystem.authservice.entity;

import com.authsystem.authservice.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(unique = true, nullable = false)
        private String username;

        @Column(unique = true, nullable = false)
        private String email;

        @Column(nullable = false)
        @JsonIgnore
        private String password;

        @Enumerated(EnumType.STRING)
        private Role role;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @Column(unique = true)
        private String resetToken;

        private LocalDateTime resetTokenExpiry;

        @PrePersist
        protected void onCreate() {
                createdAt = LocalDateTime.now();
        }

        private String verificationToken;
        private LocalDateTime verificationTokenExpiry;
        private boolean enabled = false;

        private int failedLoginAttempts;
        private LocalDateTime lockTime;

        private String lastLoginIp;
        private LocalDateTime lastLoginTime;

        private boolean deleted = false;
        private LocalDateTime suspensionEndTime;
}
package com.authsystem.authservice.repository;

import com.authsystem.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
        Optional<User> findByUsername(String username);
        Optional<User> findByEmail(String email);
        Optional<User> findByResetToken(String resetToken);
        Optional<User> findByVerificationToken(String token);
        List<User> findByEnabledFalseAndVerificationTokenExpiryBefore(LocalDateTime expiryDate);

        boolean existsByUsername(String username);
        boolean existsByEmail(String email);
}
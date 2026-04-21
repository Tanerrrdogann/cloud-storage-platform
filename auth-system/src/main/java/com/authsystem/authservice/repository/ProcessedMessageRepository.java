package com.authsystem.authservice.repository;

import com.authsystem.authservice.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {

    boolean existsByIdempotencyKeyAndStatus(String idempotencyKey, String status);

    Optional<ProcessedMessage> findByIdempotencyKey(String idempotencyKey);
}

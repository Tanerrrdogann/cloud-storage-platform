package com.authsystem.authservice.event;

import com.authsystem.authservice.dto.*;
import com.authsystem.authservice.entity.User;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EventFactory {

    private static final String PRODUCER = "auth-service";
    private static final int PAYLOAD_VERSION = 1;

    public UserCreatedMessage userCreated(User user) {
        return UserCreatedMessage.builder()
                .eventId(newEventId())
                .eventType("user.created")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.created:" + user.getId())
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    public UserLoggedInMessage userLoggedIn(User user, String ipAddress) {
        String correlationId = correlationId();

        return UserLoggedInMessage.builder()
                .eventId(newEventId())
                .eventType("user.loggedin")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.loggedin:" + user.getId() + ":" + fallback(correlationId))
                .correlationId(correlationId)
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .ipAddress(ipAddress)
                .build();
    }

    public UserLoggedOutMessage userLoggedOut(User user) {
        return UserLoggedOutMessage.builder()
                .eventId(newEventId())
                .eventType("user.loggedout")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.loggedout:" + user.getId())
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public ForgotPasswordMessage forgotPassword(User user, String resetToken) {
        return ForgotPasswordMessage.builder()
                .eventId(newEventId())
                .eventType("user.forgotpassword")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(user.getEmail())
                .idempotencyKey("user.forgotpassword:" + user.getEmail() + ":" + resetToken)
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .email(user.getEmail())
                .resetToken(resetToken)
                .build();
    }

    public UserLockedMessage userLocked(User user, String ipAddress) {
        return UserLockedMessage.builder()
                .eventId(newEventId())
                .eventType("user.locked")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.locked:" + user.getId() + ":" + fallback(ipAddress))
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .email(user.getEmail())
                .ipAddress(ipAddress)
                .build();
    }

    public UserVerifiedMessage userVerified(User user) {
        return UserVerifiedMessage.builder()
                .eventId(newEventId())
                .eventType("user.verified")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.verified:" + user.getId())
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public PasswordResetMessage passwordReset(User user) {
        return PasswordResetMessage.builder()
                .eventId(newEventId())
                .eventType("user.passwordreset")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.passwordreset:" + user.getId())
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public UserSuspendedMessage userSuspended(User user, int days, String suspendedBy) {
        return UserSuspendedMessage.builder()
                .eventId(newEventId())
                .eventType("user.suspended")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.suspended:" + user.getId() + ":" + days)
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .suspendedDays(days)
                .suspendedBy(suspendedBy)
                .build();
    }

    public UserDeletedMessage userDeleted(User user) {
        return UserDeletedMessage.builder()
                .eventId(newEventId())
                .eventType("user.deleted")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.deleted:" + user.getId())
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    public UserRoleUpdatedMessage userRoleUpdated(User user, String newRole) {
        return UserRoleUpdatedMessage.builder()
                .eventId(newEventId())
                .eventType("user.role.updated")
                .occurredAt(now())
                .producer(PRODUCER)
                .aggregateId(String.valueOf(user.getId()))
                .idempotencyKey("user.role.updated:" + user.getId() + ":" + newRole)
                .correlationId(correlationId())
                .payloadVersion(PAYLOAD_VERSION)
                .userId(user.getId())
                .newRole(newRole)
                .build();
    }

    private String newEventId() {
        return UUID.randomUUID().toString();
    }

    private Instant now() {
        return Instant.now();
    }

    private String correlationId() {
        return MDC.get("correlationId");
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}

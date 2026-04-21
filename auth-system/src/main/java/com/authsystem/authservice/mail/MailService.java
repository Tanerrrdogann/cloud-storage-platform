package com.authsystem.authservice.mail;

public interface MailService {
        void sendResetPasswordEmail(String toEmail, String resetLink);
        void sendVerificationEmail(String toEmail, String verificationLink);
}
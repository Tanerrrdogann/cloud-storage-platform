package com.authsystem.authservice.mail;

import com.authsystem.authservice.exception.MailServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    @Async
    public void sendResetPasswordEmail(String toEmail, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Hesap Güvenliği: Şifre Sıfırlama Talebi");
            message.setText(
                    "Şifrenizi sıfırlamak için tıklayın: \n" +
                    resetLink + "\n\n" +
                    "Linkiniz 30 dakika geçerlidir."
            );

            mailSender.send(message);
            
            log.info("ST_LOG | ACTION:MAIL_SENT | TYPE:PASSWORD_RESET | TO:{} | STATUS:SUCCESS", toEmail);

        } catch (Exception e) {
            log.error("ST_LOG | ACTION:MAIL_FAILED | TYPE:PASSWORD_RESET | TO:{} | MSG:{}", toEmail, e.getMessage());
            throw new MailServiceException("Şifre sıfırlama e-postası gönderilirken bir hata oluştu.");
        }
    }

    @Override
    @Async
    public void sendVerificationEmail(String toEmail, String verifyLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Hoş Geldiniz: Hesap Doğrulama İşlemi");
            message.setText(
                    "Hesabınızı aktif etmek için tıklayın: \n" +
                    verifyLink + "\n\n" +
                    "Linkiniz 30 dakika geçerlidir."
            );

            mailSender.send(message);

            log.info("ST_LOG | ACTION:MAIL_SENT | TYPE:VERIFICATION | TO:{} | STATUS:SUCCESS", toEmail);

        } catch (Exception e) {
            log.error("ST_LOG | ACTION:MAIL_FAILED | TYPE:VERIFICATION | TO:{} | MSG:{}", toEmail, e.getMessage());
            throw new MailServiceException("Doğrulama e-postası gönderilirken bir hata oluştu.");
        }
    }
}

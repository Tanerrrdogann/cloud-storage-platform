package com.authsystem.authservice;

import com.authsystem.authservice.exception.InternalServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class AuthServiceApplication {

    public static void main(String[] args) {
        try {
            log.info("ST_LOG | ACTION:APPLICATION_STARTING | STATUS:INIT");
            SpringApplication.run(AuthServiceApplication.class, args);
            log.info("ST_LOG | ACTION:APPLICATION_STARTED | STATUS:SUCCESS | VERSION:v1");
        } catch (Exception e) {
            log.error("ST_LOG | TYPE:SYSTEM_CRITICAL | ACTION:APPLICATION_FAILED_TO_START | MSG:{}", e.getMessage());
            throw new InternalServerException("Uygulama başlatılamadı: " + e.getMessage());
        }
    }
}

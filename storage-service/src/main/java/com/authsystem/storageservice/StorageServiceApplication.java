package com.authsystem.storageservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class StorageServiceApplication {

    public static void main(String[] args) {
        try {
            log.info("ST_LOG | ACTION:STORAGE_APPLICATION_STARTING | STATUS:INIT");
            SpringApplication.run(StorageServiceApplication.class, args);
            log.info("ST_LOG | ACTION:STORAGE_APPLICATION_STARTED | STATUS:SUCCESS | VERSION:v1");
        } catch (Exception ex) {
            log.error(
                    "ST_LOG | TYPE:SYSTEM_CRITICAL | ACTION:STORAGE_APPLICATION_FAILED_TO_START | MSG:{}",
                    ex.getMessage()
            );
            throw ex;
        }
    }
}

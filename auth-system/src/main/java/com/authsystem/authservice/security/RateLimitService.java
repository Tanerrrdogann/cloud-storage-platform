package com.authsystem.authservice.security;

import com.authsystem.authservice.exception.InternalServerException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.command.CommandAsyncExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class RateLimitService {

    private final RedissonBasedProxyManager<String> proxyManager;

    public RateLimitService(RedissonClient redissonClient) {
        CommandAsyncExecutor commandExecutor = ((Redisson) redissonClient).getCommandExecutor();
        
        this.proxyManager = RedissonBasedProxyManager.builderFor(commandExecutor)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(15)))
                .build();
                
        log.info("ST_LOG | ACTION:RATE_LIMIT_INIT | STATUS:SUCCESS | ENGINE:REDISSON_BUCKET4J");
    }

    public Bucket resolveBucket(String ipAddress, String actionType) {
        if (ipAddress == null || actionType == null || actionType.isBlank()) {
            log.error("ST_LOG | TYPE:SYSTEM_ERROR | ACTION:RATE_LIMIT_RESOLVE | MSG:IP_OR_ACTION_NULL");
            throw new InternalServerException("Rate limit belirlenirken IP veya işlem tipi eksik.");
        }

        String key = "rate_limit:" + ipAddress + ":" + actionType.toUpperCase();
        BucketConfiguration config = createBucketConfig(actionType);

        log.debug("ST_LOG | ACTION:BUCKET_RESOLVED | IP:{} | TYPE:{}", ipAddress, actionType);

        return proxyManager.builder().build(key, config);
    }

    private BucketConfiguration createBucketConfig(String actionType) {
        return switch (actionType.toUpperCase()) {
            case "LOGIN", "REGISTER" ->
                    BucketConfiguration.builder().addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))).build();
            case "FORGOT_PASSWORD" ->
                    BucketConfiguration.builder().addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofMinutes(1)))).build();
            case "VERIFY", "RESET_PASSWORD" ->
                    BucketConfiguration.builder().addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(15)))).build();
            case "REFRESH" ->
                    BucketConfiguration.builder().addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(10)))).build();
            default ->
                    BucketConfiguration.builder().addLimit(Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)))).build();
        };
    }
}
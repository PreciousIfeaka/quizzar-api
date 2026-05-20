package com.quizzar.ratelimit.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public ProxyManager<String> proxyManager(RedissonClient redissonClient) {
        return Bucket4jRedisson.casBasedBuilder(
                ((Redisson) redissonClient).getCommandExecutor()
        ).build();
    }
    
    public static Bandwidth aiEndpointBandwidth() {
        return Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
    }
    
    public static Bandwidth generalEndpointBandwidth() {
        return Bandwidth.builder()
                .capacity(120)
                .refillGreedy(120, Duration.ofMinutes(1))
                .build();
    }
    
    public static Bandwidth publicEndpointBandwidth() {
        return Bandwidth.builder()
                .capacity(60)
                .refillGreedy(60, Duration.ofMinutes(1))
                .build();
    }
    
    public static Bandwidth publicSubmitBandwidth() {
        return Bandwidth.builder()
                .capacity(30)
                .refillGreedy(30, Duration.ofMinutes(1))
                .build();
    }
}

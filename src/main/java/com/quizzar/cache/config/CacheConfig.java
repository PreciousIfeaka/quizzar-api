package com.quizzar.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    public static final String QUIZ_DETAIL = "quiz-detail";
    public static final String PUBLIC_QUIZ = "public-quiz";
    public static final String TEACHER_PROFILE = "teacher-profile";
    public static final String QUIZ_LIST = "quiz-list";
    public static final String ANALYTICS = "analytics";

    private final ObjectMapper objectMapper;


    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper redisObjectMapper = objectMapper.copy();
        
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
                
        redisObjectMapper.activateDefaultTyping(ptv, 
                ObjectMapper.DefaultTyping.NON_FINAL, 
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer))
                .disableCachingNullValues();
        
        return builder -> builder
            .withCacheConfiguration(QUIZ_DETAIL,
                defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(PUBLIC_QUIZ,
                defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration(TEACHER_PROFILE,
                defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(QUIZ_LIST,
                defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(ANALYTICS,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));
    }
}

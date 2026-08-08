package com.musicpod.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.musicpod.catalog.track.TrackResponse;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            JsonMapper jsonMapper) {

        JacksonJsonRedisSerializer<TrackResponse>
                trackSerializer =
                new JacksonJsonRedisSerializer<>(
                        jsonMapper,
                        TrackResponse.class
                );

        RedisCacheConfiguration trackCacheConfiguration =
                RedisCacheConfiguration
                        .defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .disableCachingNullValues()
                        .computePrefixWith(
                                cacheName ->
                                        "musicpod::"
                                                + cacheName
                                                + "::"
                        )
                        .serializeKeysWith(
                                RedisSerializationContext
                                        .SerializationPair
                                        .fromSerializer(
                                                new StringRedisSerializer()
                                        )
                        )
                        .serializeValuesWith(
                                RedisSerializationContext
                                        .SerializationPair
                                        .fromSerializer(
                                                trackSerializer
                                        )
                        );

        return RedisCacheManager
                .builder(connectionFactory)
                .cacheDefaults(
                        trackCacheConfiguration
                )
                .withCacheConfiguration(
                        CacheNames.TRACKS,
                        trackCacheConfiguration
                )
                .transactionAware()
                .enableStatistics()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler(true);
    }
}
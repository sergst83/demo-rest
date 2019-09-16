package ru.bis.rest.config;

import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    public static final String CACHE_NAME = "operation";


    @Bean
    Cache operationCache() {
        return new ConcurrentMapCache(CACHE_NAME);
    }

}
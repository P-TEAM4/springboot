package com.lol.highlight.global.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 로컬 캐시 설정
 * - DB 영구 저장 방식을 사용하므로 메모리 캐시는 최소화
 * - ConcurrentMapCache 사용 (간단한 인메모리 캐시)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("matchList", "matchDetail");
    }
}

package kr.hhplus.be.server.common.config.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;



/**
 * Spring Cache 설정 (Redis 분산 캐시 사용)
 *
 * 과제 요구사항에 맞춰 기존 Caffeine(로컬 캐시) 대신 Redis를 Spring Cache 백엔드로 사용한다.
 *
 * [실무에서 흔한 Redis 캐시 전략]
 * - TTL 기반 캐시(예: 5분)
 * - key prefix로 서비스 구분(hhplus::...)
 * - JSON 직렬화(GenericJackson2JsonRedisSerializer) + JavaTimeModule(날짜/시간 안전)
 * - transactionAware: 트랜잭션 커밋 이후 캐시 반영(쓰기 로직이 생겼을 때 안전)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 콘서트 목록 + 스케줄 목록 캐시 */
    public static final String CONCERT_LIST_CACHE = "concertList";

    /**
     * Redis 캐시 TTL
     * - 콘서트/회차는 실시간 변경 데이터가 아니므로 5분 TTL은 실무에서 흔한 균형점
     */
    private static final Duration CONCERT_LIST_TTL = Duration.ofMinutes(5);

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 공통 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer()))
                // 운영에서 키 충돌 방지(서비스 prefix)
                .computePrefixWith(cacheName -> "hhplus::" + cacheName + "::");

        // 캐시별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(CONCERT_LIST_CACHE, defaultConfig.entryTtl(CONCERT_LIST_TTL));

        // put/evict 동작을 Redis 락으로 감싸 일관성/경합 문제를 줄인다.
        RedisCacheWriter cacheWriter = RedisCacheWriter.lockingRedisCacheWriter(connectionFactory);

        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }

    /**
     * Redis value serializer
     * - DTO에 LocalDateTime이 포함되어 있어(JavaTimeModule) 없이 직렬화하면 깨질 수 있다.
     */
    private GenericJackson2JsonRedisSerializer redisValueSerializer() {
        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new GenericJackson2JsonRedisSerializer(om);
    }
}


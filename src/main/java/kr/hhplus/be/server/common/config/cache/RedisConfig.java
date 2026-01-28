package kr.hhplus.be.server.common.config.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 기본 설정
 * 
 * - RedisTemplate: 분산락용 (String-String)
 * - CacheManager: Spring Cache용 (별도 CacheConfig에서 설정)
 */
@Configuration
public class RedisConfig {

    /**
     * 분산락용 RedisTemplate
     * - Key/Value 모두 String 직렬화
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key/Value 모두 String 직렬화
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        return template;
    }
}

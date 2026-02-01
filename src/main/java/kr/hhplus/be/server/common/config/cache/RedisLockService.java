package kr.hhplus.be.server.common.config.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 분산락 구현
 * 
 * [핵심 개념]
 * - SETNX(SET if Not eXists) 명령으로 락 획득
 * - TTL로 락 자동 해제 (데드락 방지)
 * - try-finally로 락 명시적 해제
 * 
 * [DB Transaction과의 관계]
 * - 분산락 획득 → DB Transaction 시작 → 비즈니스 로직 → DB Transaction 커밋 → 분산락 해제
 * - 락을 먼저 잡아야 여러 서버/스레드가 동시에 같은 자원을 수정하지 않음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 분산락 획득 (폴링 방식)
     * 
     * @param lockKey 락 키 (예: seat:lock:1:10)
     * @param waitTimeSeconds 락 획득 시도 시간 (초)
     * @param leaseTimeSeconds 락 자동 해제 시간 (초) - 데드락 방지용
     * @return 락 획득 성공 여부
     */
    public boolean tryLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds) {
        long startTime = System.currentTimeMillis();
        long waitTimeMillis = TimeUnit.SECONDS.toMillis(waitTimeSeconds);

        try {
            while (System.currentTimeMillis() - startTime < waitTimeMillis) {
                // SETNX: key가 없으면 set하고 true 반환, 이미 있으면 false 반환
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(leaseTimeSeconds));

                if (Boolean.TRUE.equals(acquired)) {
                    log.debug("분산락 획득 성공: {}", lockKey);
                    return true;
                }

                // 100ms 대기 후 재시도 (폴링 간격)
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("분산락 획득 중 인터럽트 발생: {}", lockKey, e);
            return false;
        }

        log.warn("분산락 획득 실패 (타임아웃): {}", lockKey);
        return false;
    }

    /**
     * 분산락 해제
     * 
     * @param lockKey 락 키
     */
    public void unlock(String lockKey) {
        Boolean deleted = redisTemplate.delete(lockKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("분산락 해제 성공: {}", lockKey);
        } else {
            log.warn("분산락 해제 실패 (이미 만료됨): {}", lockKey);
        }
    }

    /**
     * 락 키 생성 헬퍼 메서드
     * 
     * @param resource 리소스 타입 (예: "seat")
     * @param identifiers 식별자들 (예: concertScheduleId, seatId)
     * @return 락 키 (예: "lock:seat:1:10")
     */
    public static String generateLockKey(String resource, Object... identifiers) {
        StringBuilder sb = new StringBuilder("lock:").append(resource);
        for (Object id : identifiers) {
            sb.append(":").append(id);
        }
        return sb.toString();
    }
}

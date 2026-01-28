package kr.hhplus.be.server.concert.service;

import kr.hhplus.be.server.concert.dto.ConcertDto.ConcertListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 캐시 성능 비교 테스트
 * 
 * [테스트 목적]
 * - 캐시 미적용 시 vs 캐시 적용 시 성능 비교
 * - 반복 조회 시 응답 시간 개선 효과 측정
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcertCachePerformanceTest {

    @Autowired
    private ConcertService concertService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Redis 캐시 적용 전후 성능 비교 테스트")
    void testCachePerformance() {
        // Given: 캐시 초기화
        clearAllCaches();

        // When & Then: 캐시 미적용 (첫 호출 - DB 조회)
        long noCacheAvgTime = measureAverageTime(10, true);
        System.out.println("\n=== 캐시 미적용 (매번 DB 조회) ===");
        System.out.println("평균 응답 시간: " + noCacheAvgTime + "ms");

        // When & Then: 캐시 적용 (Redis에서 조회)
        long withCacheAvgTime = measureAverageTime(10, false);
        System.out.println("\n=== 캐시 적용 (Redis 조회) ===");
        System.out.println("평균 응답 시간: " + withCacheAvgTime + "ms");

        // 성능 개선율 계산
        double improvementRate = ((double) (noCacheAvgTime - withCacheAvgTime) / noCacheAvgTime) * 100;
        System.out.println("\n=== 성능 개선 결과 ===");
        System.out.println("개선율: " + String.format("%.2f", improvementRate) + "%");
        System.out.println("응답 시간 단축: " + (noCacheAvgTime - withCacheAvgTime) + "ms");

        // 캐시 적용 시 응답 시간이 더 빨라야 함
        assertThat(withCacheAvgTime).isLessThan(noCacheAvgTime);
    }

    @Test
    @DisplayName("대량 요청 시 Redis 캐시 성능 측정")
    void testCachePerformanceWithHighLoad() {
        // Given
        clearAllCaches();

        int requestCount = 100;
        
        // 첫 호출로 캐시 워밍업
        concertService.getConcertList();

        // When: 캐시 미적용 시뮬레이션 (매번 캐시 초기화)
        long startNoCacheTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            clearAllCaches();
            concertService.getConcertList();
        }
        long noCacheTotalTime = System.currentTimeMillis() - startNoCacheTime;

        // When: 캐시 적용
        clearAllCaches();
        concertService.getConcertList(); // 캐시 워밍업
        
        long startWithCacheTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            concertService.getConcertList();
        }
        long withCacheTotalTime = System.currentTimeMillis() - startWithCacheTime;

        // Then: 결과 출력
        System.out.println("\n=== 대량 요청 성능 테스트 (요청 수: " + requestCount + "회) ===");
        System.out.println("캐시 미적용 총 시간: " + noCacheTotalTime + "ms");
        System.out.println("캐시 미적용 평균: " + (noCacheTotalTime / requestCount) + "ms/req");
        System.out.println("캐시 적용 총 시간: " + withCacheTotalTime + "ms");
        System.out.println("캐시 적용 평균: " + (withCacheTotalTime / requestCount) + "ms/req");
        
        double throughputImprovement = ((double) noCacheTotalTime / withCacheTotalTime);
        System.out.println("처리량 개선: " + String.format("%.2f", throughputImprovement) + "배");

        // 캐시 적용 시 전체 처리 시간이 훨씬 짧아야 함
        assertThat(withCacheTotalTime).isLessThan(noCacheTotalTime);
    }

    @Test
    @DisplayName("동시 요청 시 캐시 히트율 측정")
    void testCacheHitRatio() {
        // Given
        clearAllCaches();

        // When: 첫 호출 (캐시 미스)
        long firstCallTime = measureSingleCall();
        System.out.println("\n=== 캐시 히트율 테스트 ===");
        System.out.println("첫 번째 호출 (캐시 미스, DB 조회): " + firstCallTime + "ms");

        // When: 이후 호출들 (캐시 히트)
        List<Long> cachedCallTimes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            long callTime = measureSingleCall();
            cachedCallTimes.add(callTime);
            System.out.println((i + 2) + "번째 호출 (캐시 히트): " + callTime + "ms");
        }

        // Then: 평균 계산
        double avgCachedTime = cachedCallTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        System.out.println("\n캐시 히트 평균 응답 시간: " + String.format("%.2f", avgCachedTime) + "ms");
        System.out.println("성능 개선: " + String.format("%.2f", (double) firstCallTime / avgCachedTime) + "배");

        // 캐시 히트 시 응답 시간이 첫 호출보다 빨라야 함
        assertThat(avgCachedTime).isLessThan(firstCallTime);
    }

    /**
     * 평균 응답 시간 측정
     * 
     * @param iterations 반복 횟수
     * @param clearCache 매번 캐시 초기화 여부
     * @return 평균 응답 시간 (ms)
     */
    private long measureAverageTime(int iterations, boolean clearCache) {
        List<Long> times = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            if (clearCache) {
                clearAllCaches();
            }

            long startTime = System.nanoTime();
            List<ConcertListResponse> result = concertService.getConcertList();
            long endTime = System.nanoTime();

            long duration = (endTime - startTime) / 1_000_000; // ms로 변환
            times.add(duration);

            // 결과가 비어있지 않은지 확인
            assertThat(result).isNotEmpty();
        }

        return times.stream()
                .mapToLong(Long::longValue)
                .sum() / iterations;
    }

    /**
     * 단일 호출 응답 시간 측정
     * 
     * @return 응답 시간 (ms)
     */
    private long measureSingleCall() {
        long startTime = System.nanoTime();
        concertService.getConcertList();
        long endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000;
    }

    /**
     * 모든 캐시 초기화
     */
    private void clearAllCaches() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }
}

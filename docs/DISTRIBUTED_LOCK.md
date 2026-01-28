# Redis 분산락 적용 문서

## 개요

좌석 예약 API에 Redis 기반 분산락을 적용하여 DB 비관적 락(`@Lock(PESSIMISTIC_WRITE)`)을 대체했습니다.

## 구현 내용

### 1. Redis 분산락 서비스 구현

#### RedisLockService
- **위치**: `kr.hhplus.be.server.common.config.cache.RedisLockService`
- **핵심 메서드**:
  - `tryLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds)`: 락 획득 (폴링 방식)
  - `unlock(String lockKey)`: 락 해제
  - `generateLockKey(String resource, Object... identifiers)`: 락 키 생성 헬퍼

- **동작 방식**:
  - Redis의 `SETNX` (SET if Not eXists) 명령 사용
  - 폴링 간격: 100ms
  - TTL 설정으로 데드락 방지

#### RedisConfig
- **위치**: `kr.hhplus.be.server.common.config.cache.RedisConfig`
- `RedisTemplate<String, String>` Bean 등록 (분산락용)

### 2. 적절한 락 키 선정

```java
String lockKey = "lock:seat:{concertScheduleId}:{seatId}"
```

**키 전략 설명**:
- **리소스 타입**: `seat` (좌석 예약)
- **식별자**: `{concertScheduleId}:{seatId}`
- **이유**: 
  - 콘서트 회차별, 좌석별로 독립적인 락
  - 동시성 극대화 (다른 좌석은 동시 예약 가능)
  - 세밀한(granular) 락 범위로 성능 최적화

### 3. 적절한 락 범위 선정

#### SeatService.seatReservationRequest()

```
[분산락 획득]
    ↓
[DB Transaction 시작] (@Transactional)
    ↓
[1. 대기열 토큰 검증]
    ↓
[2. 회원 조회]
    ↓
[3. 좌석 조회 및 상태 확인]
    ↓
[4. 좌석 LOCKED 처리]
    ↓
[5. 결제 대기 생성]
    ↓
[DB Transaction 커밋]
    ↓
[분산락 해제] (finally)
```

**적용 범위 설명**:
- **범위**: 좌석 예약의 전체 트랜잭션 (좌석 상태 확인 → LOCKED 처리 → 결제 대기 생성)
- **이유**:
  - 좌석 상태 확인부터 LOCKED 처리까지 원자성 보장 필요
  - 두 트랜잭션이 동시에 같은 좌석을 AVAILABLE로 읽는 것을 방지
  - 결제 대기 생성까지 포함하여 일관성 보장

### 4. 분산락과 DB Transaction의 관계

#### 올바른 순서
```java
public void seatReservationRequest(...) {
    String lockKey = RedisLockService.generateLockKey("seat", scheduleId, seatId);
    
    // 1. 분산락 획득 (Redis)
    boolean acquired = redisLockService.tryLock(lockKey, 5, 10);
    
    if (!acquired) {
        throw new DistributedLockException("락 획득 실패");
    }
    
    try {
        // 2. DB Transaction 시작 및 비즈니스 로직
        seatReservationWithTransaction(requestDto, queueToken, userDetails);
    } finally {
        // 3. 분산락 해제 (반드시 실행)
        redisLockService.unlock(lockKey);
    }
}

@Transactional
public void seatReservationWithTransaction(...) {
    // DB Transaction 범위
    // 좌석 조회, 상태 변경, 결제 대기 생성
}
```

#### 주의사항

**❌ 잘못된 방식 (Transaction 안에서 락 획득)**
```java
@Transactional
public void wrongApproach() {
    boolean acquired = redisLockService.tryLock(lockKey, 5, 10);
    // 문제: Transaction이 먼저 시작됨
    // DB 락과 Redis 락이 섞여서 데드락 가능성 증가
}
```

**✅ 올바른 방식 (락 안에서 Transaction)**
```java
public void correctApproach() {
    boolean acquired = redisLockService.tryLock(lockKey, 5, 10);
    try {
        transactionMethod(); // @Transactional
    } finally {
        redisLockService.unlock(lockKey);
    }
}
```

**이유**:
- 분산락이 DB Transaction보다 먼저 획득되어야 함
- 분산락이 더 넓은 범위를 커버해야 동시성 제어가 올바르게 동작
- Transaction 커밋 후에도 락이 유지되어 다른 스레드가 stale data를 읽지 않도록 보장

### 5. 타임아웃 설정

```java
// 락 획득 대기 시간: 5초
// 락 자동 해제 시간(TTL): 10초
boolean acquired = redisLockService.tryLock(lockKey, 5, 10);
```

**설정 근거**:
- **waitTime (5초)**: 
  - 좌석 예약 요청이 몰릴 때 적절한 대기 시간
  - 너무 길면 사용자 경험 저하, 너무 짧으면 실패율 증가
- **leaseTime (10초)**:
  - 좌석 예약 로직이 충분히 완료될 시간
  - 데드락 방지 (서버 장애 시 자동 해제)
  - 정상 흐름에서는 finally에서 명시적 해제

### 6. 변경 사항 요약

#### 수정된 파일

1. **SeatRepository.java**
   - ❌ 제거: `@Lock(LockModeType.PESSIMISTIC_WRITE)`
   - ✅ 추가: 주석으로 Redis 분산락으로 대체했음을 명시

2. **SeatService.java**
   - ❌ 제거: `@Transactional`이 붙은 단일 메서드
   - ✅ 추가: 
     - `seatReservationRequest()`: 분산락 획득/해제 처리
     - `seatReservationWithTransaction()`: DB Transaction 범위

#### 새로 생성된 파일

1. **RedisLockService.java**: 분산락 서비스
2. **RedisConfig.java**: Redis 설정
3. **DistributedLockException.java**: 분산락 예외
4. **SeatRedisDistributedLockTest.java**: 통합 테스트

### 7. 통합 테스트

#### 테스트 시나리오

**파일**: `SeatRedisDistributedLockTest.java`

1. **동시성 테스트**
   - 100명의 사용자가 동시에 같은 좌석 예약 시도
   - Redis 분산락으로 동시성 제어
   - 검증:
     - 1명만 성공, 99명은 `DistributedLockException` 발생
     - DB에 1개의 예약만 저장됨
     - 좌석 상태가 `LOCKED`로 변경됨

2. **락 획득/해제 테스트**
   - 락 획득 성공 확인
   - 이미 락이 있을 때 획득 실패 확인
   - 락 해제 후 다시 획득 가능 확인

3. **TTL 자동 해제 테스트**
   - 락 TTL(2초) 설정
   - TTL 이전에는 획득 실패
   - TTL 이후에는 자동 해제되어 획득 성공

#### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 분산락 테스트만
./gradlew test --tests SeatRedisDistributedLockTest
```

### 8. 분산락의 장점 (vs DB 비관적 락)

| 항목 | DB 비관적 락 | Redis 분산락 |
|------|--------------|--------------|
| **확장성** | DB 서버에 부하 집중 | Redis로 부하 분산 |
| **성능** | DB 락 대기로 성능 저하 | Redis가 더 빠름 |
| **범위** | DB Row 단위로만 가능 | 임의의 리소스에 적용 가능 |
| **데드락** | DB 데드락 가능성 | TTL로 자동 해제 |
| **다중 서버** | 단일 DB로 제한적 | 분산 환경에 적합 |

### 9. 운영 고려사항

#### Redis 장애 시나리오

**문제**: Redis가 다운되면 락 획득 불가
**해결책**:
- Redis Cluster 또는 Sentinel 구성 (HA)
- Circuit Breaker 패턴 적용
- Fallback: DB 락으로 전환 (degradation)

#### 모니터링

```java
@Slf4j
public class RedisLockService {
    // 락 획득/해제 로그 기록
    log.debug("분산락 획득 성공: {}", lockKey);
    log.warn("분산락 획득 실패 (타임아웃): {}", lockKey);
}
```

**모니터링 지표**:
- 락 획득 실패율
- 락 대기 시간
- 락 보유 시간
- TTL 자동 해제 빈도

## 결론

Redis 분산락을 적용하여:
1. ✅ DB 부하 감소
2. ✅ 확장성 향상 (다중 서버 환경 대응)
3. ✅ 적절한 락 범위 설정 (콘서트 회차 + 좌석 단위)
4. ✅ DB Transaction과의 올바른 관계 설정
5. ✅ 동시성 제어 검증 (통합 테스트)

과제 요구사항을 모두 충족하며, 실무에서도 적용 가능한 수준의 분산락을 구현했습니다.

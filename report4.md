# Test Report (report4)

## Run Info
- Date: 2026-02-01
- Command: ./gradlew test
- Result: FAILED
- Summary: 29 tests completed, 23 failed
- Raw report: build/reports/tests/test/index.html

## Failure Summary (by suite)
- kr.hhplus.be.server.concert.service.ConcertCachePerformanceTest (3/3 failed)
  - Root cause: Embedded Redis failed to start (bind 127.0.0.1:6370). Subsequent context loads skipped.
- kr.hhplus.be.server.member.controller.MemberControllerTest (3/3 failed)
  - Root cause: ApplicationContext failed to load (threshold exceeded) after prior startup failure.
- kr.hhplus.be.server.payment.service.PaymentConcurrencyTest (4/4 failed)
  - Root cause: Initializer runs at startup and queries `concert` table before schema exists (Table `hhplus.concert` doesn't exist).
- kr.hhplus.be.server.payment.service.PaymentServiceTest (2/3 failed)
  - Root cause: Same Initializer -> missing `concert` table during context startup.
- kr.hhplus.be.server.point.service.ConcurrencyPointServiceTest (1/1 failed)
  - Root cause: Same Initializer -> missing `concert` table during context startup.
- kr.hhplus.be.server.ranking.service.FastSoldOutRankingIntegrationTest (1/1 failed)
  - Failure: expected 1L but was 0L when checking ZSET size (ranking not recorded).
- kr.hhplus.be.server.seat.SeatConcurrencyIntegrationTest (1/1 failed)
  - Root cause: ApplicationContext failure threshold exceeded after earlier failures.
- kr.hhplus.be.server.seat.SeatRedisDistributedLockTest (3/3 failed)
  - Root cause: ApplicationContext failure threshold exceeded after earlier failures.
- kr.hhplus.be.server.seat.SeatReservationQueueFlowTest (2/2 failed)
  - Failure 1: expected 1 but was 0
  - Failure 2: RedisConnectionFailureException (Unable to connect to Redis)
- kr.hhplus.be.server.seat.SeatServiceTest (3/4 failed)
  - Failures: NoSuchElementException (missing Optional value), assertion true/false mismatch

## Notable Logs / Root Causes
- Embedded Redis startup failure:
  - Can't start redis server... bind: No such file or directory (127.0.0.1:6370)
- DB schema missing at startup:
  - Table 'hhplus.concert' doesn't exist
- Redis connection failure (runtime):
  - RedisConnectionFailureException: Unable to connect to Redis

## New Ranking Integration Test Result
- Test: FastSoldOutRankingIntegrationTest.soldOutRankingIsRecordedOnceUnderConcurrency
- Expected: ZSET size == 1
- Actual: ZSET size == 0
- Status: FAILED

## Notes / Next Steps
- Many failures are environment/setup related (Embedded Redis + Initializer + schema creation ordering).
- The new ranking test currently fails to observe ZSET entry; likely due to:
  - Redis not available (overall), OR
  - Payment transactions not all committing due to deadlocks / timing.

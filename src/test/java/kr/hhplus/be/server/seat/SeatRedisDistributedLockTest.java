package kr.hhplus.be.server.seat;

import kr.hhplus.be.server.common.config.cache.RedisLockService;
import kr.hhplus.be.server.common.exception.DistributedLockException;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.queue.domain.QueueStatus;
import kr.hhplus.be.server.queue.domain.QueueToken;
import kr.hhplus.be.server.queue.repository.QueueTokenRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.dto.SeatDto;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import kr.hhplus.be.server.seat.servcie.SeatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 분산락 통합 테스트
 * 
 * [테스트 목적]
 * - 동시에 여러 사용자가 같은 좌석을 예약할 때 분산락이 정상 동작하는지 검증
 * - 오직 1명만 예약에 성공해야 함 (나머지는 실패)
 * 
 * [테스트 시나리오]
 * 1. 100명의 사용자가 동시에 같은 좌석 예약 시도
 * 2. Redis 분산락으로 동시성 제어
 * 3. 1명만 성공, 99명은 DistributedLockException 발생
 * 4. DB에는 1개의 예약만 저장됨
 */
@SpringBootTest
@ActiveProfiles("test")
class SeatRedisDistributedLockTest {

    @Autowired
    private SeatService seatService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    private QueueTokenRepository queueTokenRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisLockService redisLockService;

    private Concert concert;
    private ConcertSchedule schedule;
    private Seat seat;
    private List<Member> members;
    private List<QueueToken> queueTokens;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        paymentRepository.deleteAll();
        seatRepository.deleteAll();
        concertScheduleRepository.deleteAll();
        concertRepository.deleteAll();
        queueTokenRepository.deleteAll();
        memberRepository.deleteAll();

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        // 콘서트 생성
        concert = new Concert();
        concert.setVenueName("테스트 콘서트");
        concert.setPrice(50000);
        concert = concertRepository.save(concert);

        // 콘서트 회차 생성
        schedule = new ConcertSchedule();
        schedule.setConcert(concert);
        schedule.setConcertDate(LocalDateTime.now().plusDays(7));
        schedule = concertScheduleRepository.save(schedule);

        // 좌석 생성 (1개만 생성, 동시성 테스트용)
        seat = new Seat();
        seat.setSchedule(schedule);
        seat.setSeatNumber(1);
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat = seatRepository.save(seat);

        // 100명의 회원 생성
        members = new ArrayList<>();
        queueTokens = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Member member = new Member();
            member.setMemberId("user" + i);
            member.setPassword("password");
            member.setMemberName("사용자" + i);
            member = memberRepository.save(member);
            members.add(member);

            // 대기열 토큰 생성 (ACTIVE 상태)
            QueueToken queueToken = new QueueToken();
            queueToken.setMemberId(member.getMemberId());
            queueToken.setToken("token-" + i);
            queueToken.setStatus(QueueStatus.ACTIVE);
            queueToken = queueTokenRepository.save(queueToken);
            queueTokens.add(queueToken);
        }
    }

    @AfterEach
    void tearDown() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("동시에 100명이 같은 좌석을 예약할 때 Redis 분산락으로 1명만 성공해야 한다")
    void testDistributedLockConcurrency() throws InterruptedException {
        // Given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 100명이 동시에 같은 좌석 예약 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    Member member = members.get(index);
                    QueueToken queueToken = queueTokens.get(index);

                    UserDetails userDetails = User.builder()
                            .username(member.getMemberId())
                            .password(member.getPassword())
                            .build();

                    SeatDto.seatReservationRequestDto requestDto = new SeatDto.seatReservationRequestDto(
                            seat.getSeatId(),
                            schedule.getConcertScheduleId()
                    );

                    seatService.seatReservationRequest(requestDto, queueToken.getToken(), userDetails);
                    successCount.incrementAndGet();

                } catch (DistributedLockException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 1명만 성공, 99명은 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(99);

        // DB에도 1개의 예약만 저장되어 있어야 함
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);

        // 좌석 상태가 LOCKED로 변경되어 있어야 함
        Seat lockedSeat = seatRepository.findById(seat.getSeatId()).orElseThrow();
        assertThat(lockedSeat.getSeatStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(lockedSeat.getLockedUserId()).isNotNull();
    }

    @Test
    @DisplayName("Redis 분산락 획득 및 해제가 정상 동작해야 한다")
    void testRedisLockAcquireAndRelease() {
        // Given
        String lockKey = RedisLockService.generateLockKey("seat", 1L, 1L);

        // When: 락 획득
        boolean acquired = redisLockService.tryLock(lockKey, 3, 10);

        // Then: 락 획득 성공
        assertThat(acquired).isTrue();

        // When: 같은 키로 다시 락 획득 시도 (이미 락이 있으므로 실패)
        boolean acquiredAgain = redisLockService.tryLock(lockKey, 1, 10);

        // Then: 락 획득 실패
        assertThat(acquiredAgain).isFalse();

        // When: 락 해제
        redisLockService.unlock(lockKey);

        // When: 락 해제 후 다시 락 획득 시도
        boolean acquiredAfterUnlock = redisLockService.tryLock(lockKey, 3, 10);

        // Then: 락 획득 성공
        assertThat(acquiredAfterUnlock).isTrue();

        // Cleanup
        redisLockService.unlock(lockKey);
    }

    @Test
    @DisplayName("Redis 분산락 TTL(자동 해제)이 정상 동작해야 한다")
    void testRedisLockTTL() throws InterruptedException {
        // Given
        String lockKey = RedisLockService.generateLockKey("seat", 1L, 1L);

        // When: 락 획득 (TTL 2초)
        boolean acquired = redisLockService.tryLock(lockKey, 3, 2);
        assertThat(acquired).isTrue();

        // When: 1초 대기 (TTL 이전)
        Thread.sleep(1000);

        // Then: 아직 락이 있어서 획득 실패
        boolean acquiredDuringTTL = redisLockService.tryLock(lockKey, 1, 10);
        assertThat(acquiredDuringTTL).isFalse();

        // When: 2초 더 대기 (TTL 이후)
        Thread.sleep(2000);

        // Then: TTL로 자동 해제되어 락 획득 성공
        boolean acquiredAfterTTL = redisLockService.tryLock(lockKey, 3, 10);
        assertThat(acquiredAfterTTL).isTrue();

        // Cleanup
        redisLockService.unlock(lockKey);
    }
}

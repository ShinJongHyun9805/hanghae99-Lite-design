package kr.hhplus.be.server.ranking.service;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.domain.Role;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.payment.service.PaymentService;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.repository.PointRepository;
import kr.hhplus.be.server.ranking.redis.FastSoldOutRedisKeys;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.embedded.redis.enabled=false")
@Import(TestcontainersConfiguration.class)
class FastSoldOutRankingIntegrationTest {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    ConcertRepository concertRepository;

    @Autowired
    PointRepository pointRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private Long scheduleId;
    private Long concertId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        paymentRepository.deleteAll();
        seatRepository.deleteAll();
        concertScheduleRepository.deleteAll();
        concertRepository.deleteAll();
        pointRepository.deleteAll();
        memberRepository.deleteAll();

        Concert concert = new Concert();
        concert.setTitle("Fast Sold Out");
        concert.setVenueName("Arena A");
        concert.setPrice(10000);
        concert.setOpenYn("Y");
        concert.setCreated_dt(LocalDateTime.now());
        concert = concertRepository.save(concert);
        concertId = concert.getId();

        ConcertSchedule schedule = new ConcertSchedule();
        schedule.setConcertDate(LocalDateTime.now().plusDays(1));
        schedule.setSalesOpenAt(LocalDateTime.now().minusSeconds(20));
        schedule.setConcert(concert);
        schedule = concertScheduleRepository.save(schedule);
        scheduleId = schedule.getConcertScheduleId();

        Member member = Member.builder()
                .memberId("member-1")
                .password("pw")
                .memberName("Member One")
                .roles(Set.of(Role.MEMBER))
                .build();
        member = memberRepository.save(member);
        memberId = member.getId();

        Point point = new Point();
        point.setMemberId(String.valueOf(memberId));
        point.setPointAmt(1_000_000);
        pointRepository.save(point);
    }

    @Test
    void soldOutRankingIsRecordedOnceUnderConcurrency() throws Exception {
        int seatCount = 5;
        for (int i = 0; i < seatCount; i++) {
            Seat seat = new Seat();
            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedUserId(memberId);
            seat.setLockedAt(LocalDateTime.now());
            seat.setSchedule(concertScheduleRepository.findById(scheduleId).orElseThrow());
            seatRepository.save(seat);

            Payment payment = new Payment();
            payment.setMemberId(memberId);
            payment.setSeatId(seat.getSeatId());
            payment.setConcertId(concertId);
            payment.setConcertScheduleId(scheduleId);
            payment.setPrice(10000);
            payment.setPaymentStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
        }

        List<Payment> payments = paymentRepository.findAll();

        ExecutorService executor = Executors.newFixedThreadPool(seatCount);
        CountDownLatch ready = new CountDownLatch(seatCount);
        CountDownLatch start = new CountDownLatch(1);

        for (Payment payment : payments) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    paymentService.completePayment(payment.getId());
                } catch (Exception ignored) {
                }
            });
        }

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        String dayKey = FastSoldOutRedisKeys.rankDay(DAY_FORMATTER.format(LocalDate.now(ZoneOffset.UTC)));
        String outAtKey = FastSoldOutRedisKeys.soldOutAt(scheduleId);
        String openAtKey = FastSoldOutRedisKeys.soldOpenAt(scheduleId);

        waitForRankEntry(dayKey);

        Long rankSize = stringRedisTemplate.opsForZSet().zCard(dayKey);
        assertThat(rankSize).isEqualTo(1);

        String outAt = stringRedisTemplate.opsForValue().get(outAtKey);
        String openAt = stringRedisTemplate.opsForValue().get(openAtKey);
        assertThat(outAt).isNotNull();
        assertThat(openAt).isNotNull();

        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(dayKey, 0, -1);
        assertThat(tuples).isNotNull();
        assertThat(tuples).hasSize(1);

        ZSetOperations.TypedTuple<String> tuple = tuples.iterator().next();
        assertThat(tuple.getValue()).isEqualTo(String.valueOf(scheduleId));
        assertThat(tuple.getScore()).isNotNull();

        long duration = tuple.getScore().longValue();
        long expected = Long.parseLong(outAt) - Long.parseLong(openAt);
        assertThat(duration).isBetween(expected - 2, expected + 2);

        Payment onePayment = payments.get(0);
        paymentService.completePayment(onePayment.getId());

        String outAtAfter = stringRedisTemplate.opsForValue().get(outAtKey);
        Long rankSizeAfter = stringRedisTemplate.opsForZSet().zCard(dayKey);

        assertThat(outAtAfter).isEqualTo(outAt);
        assertThat(rankSizeAfter).isEqualTo(1);
    }

    private void waitForRankEntry(String dayKey) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Long size = stringRedisTemplate.opsForZSet().zCard(dayKey);
            if (size != null && size > 0) {
                return;
            }
            Thread.sleep(100);
        }
    }
}

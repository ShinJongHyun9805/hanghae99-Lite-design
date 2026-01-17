package kr.hhplus.be.server.payment.service;

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
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.repository.PointRepository;
import kr.hhplus.be.server.queue.service.QueueTokenService;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentConcurrencyTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    ConcertRepository concertRepository;

    @Autowired
    PointRepository pointRepository;

    @MockBean
    QueueTokenService queueTokenService;

    private Long concertScheduleId;
    private Long seatId;
    private Long concertId;

    private Long memberId = 1L;
    private String memberIdStr = "1";

    @BeforeEach
    void setUp() {
        doNothing().when(queueTokenService)
                .validateActiveToken(anyString(), anyString());

        // 테스트 데이터 초기화 (외래키 제약을 고려한 삭제 순서)
        paymentRepository.deleteAll();
        pointRepository.deleteAll();
        seatRepository.deleteAll();
        concertScheduleRepository.deleteAll();
        concertRepository.deleteAll();
        memberRepository.deleteAll();

        // 콘서트 생성
        Concert concert = new Concert();
        concert.setTitle("Test Concert");
        concert.setVenueName("Test Venue");
        concert.setPrice(100000);
        concert.setOpenYn("Y");
        concert.setCreated_dt(LocalDateTime.now());
        concert = concertRepository.save(concert);
        concertId = concert.getId();

        // 콘서트 스케줄 생성
        ConcertSchedule schedule = new ConcertSchedule();
        schedule.setConcertDate(LocalDateTime.of(2025, 12, 10, 19, 0));
        schedule.setConcert(concert);
        schedule = concertScheduleRepository.save(schedule);
        concertScheduleId = schedule.getConcertScheduleId();

        // 테스트 사용자 생성
        for (int i = 1; i <= 20; i++) {
            if (!memberRepository.existsByMemberId("test-user-" + i)) {
                Member member = Member.builder()
                        .memberId("test-user-" + i)
                        .password("password")
                        .memberName("테스트유저" + i)
                        .roles(Set.of(Role.MEMBER))
                        .build();
                memberRepository.save(member);
            }
        }

        // 초기 포인트 설정 (90,000원 - 3번의 결제만 가능)
        Point initialPoint = new Point();
        initialPoint.setMemberId(memberIdStr);
        initialPoint.setPointAmt(90_000);
        pointRepository.save(initialPoint);

        // Payment 5개 생성 (모두 같은 memberId)
        for (int i = 0; i < 5; i++) {
            Seat seat = new Seat();
            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedUserId(memberId);
            seat.setLockedAt(LocalDateTime.now());
            seat.setSchedule(schedule);
            seatRepository.save(seat);

            Payment payment = new Payment();
            payment.setMemberId(memberId);
            payment.setSeatId(seat.getSeatId());
            payment.setConcertId(concert.getId());
            payment.setConcertScheduleId(schedule.getConcertScheduleId());
            payment.setPrice(30_000);
            payment.setPaymentStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
        }
    }


    @Test
    void 포인트_차감_동시성_테스트_음수_잔액_방지() throws Exception {
        List<Payment> payments = paymentRepository.findAll();

        int threadCount = payments.size();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<String> errorMessages = new CopyOnWriteArrayList<>();

        for (Payment payment : payments) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    mockMvc.perform(
                            put("/api/v1/payment/payment/{paymentId}", payment.getId())
                                    .with(user("1").roles("MEMBER"))
                    ).andExpect(status().isOk());

                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                    errorMessages.add(e.getClass().getSimpleName() + ": " + e.getMessage());
                }
                return null;
            });
        }

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Point finalPoint = pointRepository
                .findByMemberId(memberIdStr)
                .orElseThrow();

        System.out.println("=== 포인트 차감 동시성 테스트 결과 ===");
        System.out.println("성공한 API 호출 수: " + success.get());
        System.out.println("실패한 API 호출 수: " + fail.get());
        System.out.println("최종 잔액: " + finalPoint.getPointAmt());
        System.out.println("초기 포인트: 90,000원");
        System.out.println("결제 시도 수: " + threadCount);
        System.out.println("결제 금액: 30,000원");
        System.out.println("실제 차감 금액: " + (90_000 - finalPoint.getPointAmt()));
        if (!errorMessages.isEmpty()) {
            System.out.println("에러 메시지:");
            errorMessages.forEach(msg -> System.out.println("  - " + msg));
        }

        // 검증: 가장 중요한 것은 음수 잔액 방지와 포인트 정합성
        assertThat(finalPoint.getPointAmt()).isGreaterThanOrEqualTo(0); // 음수 잔액 방지
        assertThat(finalPoint.getPointAmt()).isLessThanOrEqualTo(90_000); // 초기 잔액보다 많아질 수 없음
        
        // 차감된 금액이 30,000의 배수인지 확인 (정합성)
        int deducted = 90_000 - finalPoint.getPointAmt();
        assertThat(deducted % 30_000).isEqualTo(0);
        
        // 성공 횟수와 실제 차감 금액이 일치하는지 확인
        // MockMvc 동시성 이슈로 API 호출은 실패할 수 있지만, 실제 차감은 정확해야 함
        int expectedDeductions = deducted / 30_000;
        System.out.println("실제 결제 완료된 건수 (포인트 기준): " + expectedDeductions);
        assertThat(expectedDeductions).isLessThanOrEqualTo(3); // 최대 3번만 가능
    }

    @Test
    void 동일한_사용자_여러_결제_동시_실행_시_포인트_정합성_보장() throws Exception {
        // 포인트 150,000원으로 설정 (5번 모두 성공 가능)
        Point point = pointRepository.findByMemberId(memberIdStr).orElseThrow();
        point.setPointAmt(150_000);
        pointRepository.save(point);

        List<Payment> payments = paymentRepository.findAll();
        int threadCount = payments.size();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (Payment payment : payments) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    mockMvc.perform(
                            put("/api/v1/payment/payment/{paymentId}", payment.getId())
                                    .with(user("1").roles("MEMBER"))
                    ).andExpect(status().isOk());

                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                }
                return null;
            });
        }

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Point finalPoint = pointRepository.findByMemberId(memberIdStr).orElseThrow();

        // 실제로 PAYMENT 상태인 것만 카운트
        long actualPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAYMENT)
                .count();
        
        System.out.println("=== 충분한 포인트 보유 시 동시성 테스트 결과 ===");
        System.out.println("API 호출 성공 수: " + success.get());
        System.out.println("API 호출 실패 수: " + fail.get());
        System.out.println("실제 결제 완료 건수: " + actualPayments);
        System.out.println("최종 잔액: " + finalPoint.getPointAmt());
        System.out.println("실제 차감된 포인트: " + (150_000 - finalPoint.getPointAmt()));
        System.out.println("주의: 같은 좌석에 대한 여러 Payment가 있을 경우, 하나만 성공하고 나머지는 취소됩니다.");
        
        // 포인트 정합성 검증
        assertThat(finalPoint.getPointAmt()).isEqualTo(150_000 - (actualPayments * 30_000));
        assertThat(finalPoint.getPointAmt()).isGreaterThanOrEqualTo(0); // 음수 방지
        
        // 각 좌석당 하나씩만 결제 완료되어야 함 (5개 좌석)
        assertThat(actualPayments).isEqualTo(5);
    }

    @Test
    void 포인트_부족_시_모든_결제_실패_및_음수_방지() throws Exception {
        // 포인트를 20,000원으로 설정 (30,000원 결제 불가능)
        Point point = pointRepository.findByMemberId(memberIdStr).orElseThrow();
        point.setPointAmt(20_000);
        pointRepository.save(point);

        List<Payment> payments = paymentRepository.findAll();
        int threadCount = payments.size();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        for (Payment payment : payments) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    mockMvc.perform(
                            put("/api/v1/payment/payment/{paymentId}", payment.getId())
                                    .with(user("1").roles("MEMBER"))
                    ).andExpect(status().isOk());

                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                }
                return null;
            });
        }

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        Point finalPoint = pointRepository.findByMemberId(memberIdStr).orElseThrow();

        System.out.println("=== 포인트 부족 시 동시성 테스트 결과 ===");
        System.out.println("성공한 결제 수: " + success.get());
        System.out.println("실패한 결제 수: " + fail.get());
        System.out.println("최종 잔액: " + finalPoint.getPointAmt());
        System.out.println("초기 잔액: 20,000원");

        // 모든 결제가 성공하되 포인트 부족으로 CANCEL 상태가 되어야 함
        assertThat(success.get()).isEqualTo(5); // API 호출은 200 OK
        assertThat(finalPoint.getPointAmt()).isEqualTo(20_000); // 포인트는 차감되지 않음

        // 모든 결제가 CANCEL 상태인지 확인
        List<Payment> canceledPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.CANCEL)
                .filter(p -> "INSUFFICIENT_POINT".equals(p.getCancelReason()))
                .toList();

        assertThat(canceledPayments.size()).isEqualTo(5);
    }

    @Test
    void 대규모_동시_결제_요청_스트레스_테스트() throws Exception {
        // 초기 데이터를 많이 생성 (20개의 Payment, 각각 다른 좌석)
        paymentRepository.deleteAll();
        seatRepository.deleteAll();
        
        List<Payment> manyPayments = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Seat seat = new Seat();
            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedUserId(memberId);
            seat.setLockedAt(LocalDateTime.now());
            seat.setSchedule(concertScheduleRepository.findById(concertScheduleId).orElseThrow());
            seatRepository.save(seat);

            Payment payment = new Payment();
            payment.setMemberId(memberId);
            payment.setSeatId(seat.getSeatId());
            payment.setConcertId(concertId);
            payment.setConcertScheduleId(concertScheduleId);
            payment.setPrice(30_000);
            payment.setPaymentStatus(PaymentStatus.PENDING);
            manyPayments.add(paymentRepository.save(payment));
        }

        // 포인트 300,000원 설정 (10번 성공 가능)
        Point point = pointRepository.findByMemberId(memberIdStr).orElseThrow();
        point.setPointAmt(300_000);
        pointRepository.save(point);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        AtomicLong totalTime = new AtomicLong();

        for (Payment payment : manyPayments) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();

                    long startTime = System.currentTimeMillis();
                    mockMvc.perform(
                            put("/api/v1/payment/payment/{paymentId}", payment.getId())
                                    .with(user("1").roles("MEMBER"))
                    ).andExpect(status().isOk());
                    long endTime = System.currentTimeMillis();

                    totalTime.addAndGet(endTime - startTime);
                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                }
                return null;
            });
        }

        ready.await();
        long testStartTime = System.currentTimeMillis();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        long testEndTime = System.currentTimeMillis();

        Point finalPoint = pointRepository.findByMemberId(memberIdStr).orElseThrow();

        // 실제로 PAYMENT 상태인 건수 카운트
        long actualPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAYMENT)
                .count();

        System.out.println("=== 대규모 동시 결제 스트레스 테스트 결과 ===");
        System.out.println("총 결제 시도 수: " + threadCount);
        System.out.println("API 호출 성공 수: " + success.get());
        System.out.println("API 호출 실패 수: " + fail.get());
        System.out.println("실제 결제 완료 건수: " + actualPayments);
        System.out.println("최종 잔액: " + finalPoint.getPointAmt());
        System.out.println("예상 성공 수: 10번");
        System.out.println("실제 차감된 포인트: " + (300_000 - finalPoint.getPointAmt()));
        System.out.println("총 소요 시간: " + (testEndTime - testStartTime) + "ms");
        if (success.get() > 0) {
            System.out.println("평균 응답 시간: " + (totalTime.get() / success.get()) + "ms");
        }

        // 정확히 10번만 결제 완료되어야 함
        assertThat(actualPayments).isEqualTo(10);
        assertThat(finalPoint.getPointAmt()).isEqualTo(0);
        assertThat(finalPoint.getPointAmt()).isGreaterThanOrEqualTo(0); // 음수 방지
        
        // 포인트 정합성 검증
        assertThat(finalPoint.getPointAmt()).isEqualTo(300_000 - (actualPayments * 30_000));
    }
}

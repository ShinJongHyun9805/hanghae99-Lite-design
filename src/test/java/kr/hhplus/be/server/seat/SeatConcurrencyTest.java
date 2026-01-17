package kr.hhplus.be.server.seat;

import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.domain.Role;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class SeatConcurrencyIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    SeatRepository seatRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    ConcertRepository concertRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @MockBean
    QueueTokenService queueTokenService;

    private Long concertScheduleId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        doNothing().when(queueTokenService)
                .validateActiveToken(anyString(), anyString());

        // 테스트 데이터 초기화 (외래키 제약을 고려한 삭제 순서)
        paymentRepository.deleteAll();
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

        // 콘서트 스케줄 생성
        ConcertSchedule schedule = new ConcertSchedule();
        schedule.setConcertDate(LocalDateTime.of(2025, 12, 10, 19, 0));
        schedule.setConcert(concert);
        schedule = concertScheduleRepository.save(schedule);
        concertScheduleId = schedule.getConcertScheduleId();

        // 좌석 생성
        Seat seat = new Seat();
        seat.setSeatNumber(1);
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat.setSchedule(schedule);
        seat = seatRepository.save(seat);
        seatId = seat.getSeatId();

        // 테스트 사용자 생성 (각 스레드마다 다른 사용자)
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
    }

    @Test
    void 동시에_같은_좌석을_요청하면_한명만_성공한다() throws Exception {
        // given
        int threadCount = 20; // 더 많은 스레드로 동시성 테스트 강화
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<TestResult>> results = new ArrayList<>();
        AtomicLong startTime = new AtomicLong();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errorMessages = new CopyOnWriteArrayList<>();

        // when
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i + 1;
            final String userId = "test-user-" + threadIndex;
            
            results.add(executorService.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new TestResult(false, 0, "Interrupted", 0);
                }

                long requestStartTime = System.currentTimeMillis();
                if (startTime.get() == 0) {
                    startTime.set(requestStartTime);
                }

                try {
                    int status = mockMvc.perform(
                                    post("/api/v1/seat/request")
                                            .with(user(userId).roles("MEMBER"))
                                            .header("Queue-Token", "mock-queue-token")
                                            .contentType(APPLICATION_JSON)
                                            .content("""
                                        {
                                          "concertScheduleId": %d,
                                          "seatId": %d
                                        }
                                    """.formatted(concertScheduleId, seatId))
                            ).andReturn()
                            .getResponse()
                            .getStatus();

                    long elapsedTime = System.currentTimeMillis() - requestStartTime;
                    boolean success = status == 200;
                    
                    if (success) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                        errorMessages.add(String.format("Thread-%d: HTTP %d", threadIndex, status));
                    }

                    return new TestResult(success, status, "", elapsedTime);
                } catch (Throwable t) {
                    long elapsedTime = System.currentTimeMillis() - requestStartTime;
                    failCount.incrementAndGet();
                    String errorMsg = String.format("Thread-%d: %s", threadIndex, t.getClass().getSimpleName() + ": " + t.getMessage());
                    errorMessages.add(errorMsg);
                    return new TestResult(false, 0, errorMsg, elapsedTime);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(30, TimeUnit.SECONDS);
        
        if (!terminated) {
            executorService.shutdownNow();
        }

        long totalElapsedTime = System.currentTimeMillis() - startTime.get();

        // 결과 수집
        List<TestResult> testResults = new ArrayList<>();
        for (Future<TestResult> future : results) {
            try {
                testResults.add(future.get());
            } catch (Exception e) {
                testResults.add(new TestResult(false, 0, "Future.get() failed: " + e.getMessage(), 0));
            }
        }

        // 데이터베이스 상태 확인
        Seat finalSeat = seatRepository.findById(seatId).orElse(null);
        int lockedSeatCount = seatRepository.findAll().stream()
                .mapToInt(s -> s.getSeatStatus() == SeatStatus.LOCKED ? 1 : 0)
                .sum();

        // then
        assertThat(successCount.get()).as("성공한 요청은 정확히 1개여야 함").isEqualTo(1);
        assertThat(failCount.get()).as("실패한 요청은 %d개여야 함", threadCount - 1).isEqualTo(threadCount - 1);
        assertThat(finalSeat).as("좌석이 존재해야 함").isNotNull();
        assertThat(finalSeat.getSeatStatus()).as("좌석 상태는 LOCKED여야 함").isEqualTo(SeatStatus.LOCKED);
        assertThat(lockedSeatCount).as("LOCKED 상태인 좌석은 1개여야 함").isEqualTo(1);

        // 테스트 결과 출력 (보고서 작성용)
        System.out.println("\n========== 동시성 테스트 결과 ==========");
        System.out.println("총 스레드 수: " + threadCount);
        System.out.println("성공한 요청: " + successCount.get());
        System.out.println("실패한 요청: " + failCount.get());
        System.out.println("총 실행 시간: " + totalElapsedTime + "ms");
        System.out.println("평균 응답 시간: " + (testResults.stream().mapToLong(TestResult::elapsedTime).average().orElse(0)) + "ms");
        System.out.println("최소 응답 시간: " + testResults.stream().mapToLong(TestResult::elapsedTime).min().orElse(0) + "ms");
        System.out.println("최대 응답 시간: " + testResults.stream().mapToLong(TestResult::elapsedTime).max().orElse(0) + "ms");
        System.out.println("좌석 최종 상태: " + (finalSeat != null ? finalSeat.getSeatStatus() : "NULL"));
        if (!errorMessages.isEmpty() && errorMessages.size() <= 10) {
            System.out.println("에러 메시지 (최대 10개):");
            errorMessages.forEach(System.out::println);
        }
        System.out.println("=====================================\n");
    }

    private static class TestResult {
        final boolean success;
        final int status;
        final String errorMessage;
        final long elapsedTime;

        TestResult(boolean success, int status, String errorMessage, long elapsedTime) {
            this.success = success;
            this.status = status;
            this.errorMessage = errorMessage;
            this.elapsedTime = elapsedTime;
        }

        long elapsedTime() {
            return elapsedTime;
        }
    }
}
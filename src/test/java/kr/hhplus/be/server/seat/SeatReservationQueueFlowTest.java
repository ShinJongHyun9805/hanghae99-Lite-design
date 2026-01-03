package kr.hhplus.be.server.seat;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
public class SeatReservationQueueFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 토큰_발급_후_좌석_예약한다() throws Exception {

        String memberId = "test";
        String password = "1234qwer";
        Long concertScheduledId = 1L;
        Long seatId = 1L;

        // 1. 회원 가입.
        mockMvc.perform(post("/api/v1/member/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "memberId": "%s",
                              "password": "%s",
                              "name": "테스터"
                            }
                        """.formatted(memberId, password)))
                .andExpect(status().isCreated());


        // 2. 로그인 → JWT 발급
        String loginResponse = mockMvc.perform(post("/api/v1/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "memberId": "%s",
                              "password": "%s"
                            }
                        """.formatted(memberId, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = new ObjectMapper().readTree(loginResponse).get("accessToken").asText();

        // 3. 대기열 토큰 발급
        String queueToken = mockMvc.perform(post("/api/v1/queue/token")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 4. 좌석 예약 요청 (Queue Token 포함)
        mockMvc.perform(post("/api/v1/seat/request")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Queue-Token", queueToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "concertScheduleId": %d,
                              "seatId": %d
                            }
                        """.formatted(concertScheduledId, seatId)))
                .andExpect(status().isOk())
                .andExpect(content().string("좌석 예약에 성공했습니다."));
    }

    /**
     * 대기열 순번에 따른 대기는 구현하지 않음. 
     * */
    @Test
    void 동시에_같은_좌석을_요청하면_오직_한명만_성공한다() throws Exception {

        // given
        int userCount = 5;
        Long concertScheduleId = 1L;
        Long seatId = 1L;

        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch readyLatch = new CountDownLatch(userCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();

        // when
        for (int i = 1; i <= userCount; i++) {
            final String memberId = "user" + i;

            results.add(executor.submit(() -> {

                String accessToken = signUpAndLogin(memberId);
                String queueToken = issueQueueToken(accessToken);

                readyLatch.countDown();      // 준비 완료
                startLatch.await();          // 동시에 출발

                try {
                    int status = mockMvc.perform(post("/api/v1/seat/request")
                                    .header("Authorization", "Bearer " + accessToken)
                                    .header("Queue-Token", queueToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "concertScheduleId": %d,
                                              "seatId": %d
                                            }
                                        """.formatted(concertScheduleId, seatId)))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                    return status == 200;
                } catch (Throwable t) {
                    return false;
                }
            }));
        }

        // 모든 스레드가 준비될 때까지 대기
        readyLatch.await();
        startLatch.countDown(); // 동시에 시작

        // then
        int successCount = 0;
        int failCount = 0;

        for (Future<Boolean> result : results) {
            if (result.get()) successCount++;
            else failCount++;
        }

        executor.shutdown();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(userCount - 1);
    }

    private String signUpAndLogin(String memberId) throws Exception {

        mockMvc.perform(post("/api/v1/member/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "memberId": "%s",
                              "password": "password1234",
                              "name": "사용자"
                            }
                        """.formatted(memberId)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/api/v1/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "memberId": "%s",
                              "password": "password1234"
                            }
                        """.formatted(memberId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new ObjectMapper().readTree(loginResponse).get("accessToken").asText();
    }

    private String issueQueueToken(String accessToken) throws Exception {
        return mockMvc.perform(post("/api/v1/queue/token")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}

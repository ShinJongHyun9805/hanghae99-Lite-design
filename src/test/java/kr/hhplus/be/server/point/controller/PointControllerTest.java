package kr.hhplus.be.server.point.controller;

import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Test
    void 정상_요청이면_200_OK_포인트_정보_반환() throws Exception {

        Point point = new Point();
        point.setMemberId("shin");
        point.setPointAmt(1500);

        given(pointService.chargePoint(any())).willReturn(new PointChargeResponseDto(1L, "shin", 1500));

        mockMvc.perform(post("/api/v1/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "memberId": "shin",
                              "amount": 500
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("shin"))
                .andExpect(jsonPath("$.pointAmt").value(1500));
    }

    @Test
    void 필수값_누락__400_Bad_Request_발생() throws Exception {
        mockMvc.perform(post("/api/v1/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "amount": 500
                            }
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 포인트_충전_금액_0원_400_Bad_Request_발생() throws Exception {
        mockMvc.perform(post("/api/v1/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "memberId" : "park",
                              "amount": 0
                            }
                        """))
                .andExpect(status().isBadRequest());
    }
}

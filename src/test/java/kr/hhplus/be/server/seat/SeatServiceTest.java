package kr.hhplus.be.server.seat;

import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto.SeatResponse;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class SeatServiceTest {

    @Autowired
    private SeatRepository seatRepository;


    @Test
    void 예약가능한_좌석_조회() throws Exception {

        List<SeatResponse> result = seatRepository.findBySchedule_ConcertScheduleIdAndSeatStatusOrderBySeatNumberAsc(1L, SeatStatus.AVAILABLE)
                .stream()
                .map(e -> {
                    SeatResponse seat = new SeatResponse();
                    seat.setSeatId(e.getSeatId());
                    seat.setConcertScheduleId(e.getSchedule().getConcertScheduleId());

                    return seat;
                })
                .toList();

        Assertions.assertTrue(!result.isEmpty());
    }

}
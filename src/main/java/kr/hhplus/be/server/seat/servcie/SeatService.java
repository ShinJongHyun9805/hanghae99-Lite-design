package kr.hhplus.be.server.seat.servcie;

import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto.SeatResponse;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;


    public seatResponseDto getSeat(Long concertScheduleId) {

        List<SeatResponse> availableSeatList = seatRepository.findBySchedule_ConcertScheduleIdAndSeatStatusOrderBySeatNumberAsc(concertScheduleId, SeatStatus.AVAILABLE)
                .stream()
                .map(e -> {
                    SeatResponse seat = new SeatResponse();
                    seat.setSeatId(e.getSeatId());
                    seat.setConcertScheduleId(e.getSchedule().getConcertScheduleId());

                    return seat;
                })
                .toList();

        return new seatResponseDto(availableSeatList);
    }
}

package kr.hhplus.be.server.seat.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class SeatDto {

    public record seatResponseDto (
        List<SeatResponse> seatList
    ) {
        @Setter @Getter
        public static class SeatResponse {

            Long concertScheduleId;

            Long seatId;
        }
    }
}

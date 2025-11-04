package kr.hhplus.be.server.concert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "해당 콘서트의 예약 가능한 날짜와 예약 가능한 좌석 조회 조회 응답 DTO")
public class ConcertScheduleListResponseDto {

    @Schema(description = "콘서트 스케쥴 ID", example = "1")
    private Long concertScheduleId;

    @Schema(description = "예약 가능한 날짜", example = "2025-11-02")
    private String concertDate;

    @Schema(description = "예약 가능한 좌석 목록 (1~50)", example = "[{\"seatId\":1,\"seatStatus\":\"AVAILABLE\"}]")
    private List<SeatInfoResponseDto> seats;
}

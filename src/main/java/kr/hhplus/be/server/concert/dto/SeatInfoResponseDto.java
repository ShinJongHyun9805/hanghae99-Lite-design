package kr.hhplus.be.server.concert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "좌석 정보 DTO")
public class SeatInfoResponseDto {

    @Schema(description = "좌석 ID", example = "1")
    private Long seatId;

    @Schema(description = "좌석 상태", example = "AVAILABLE")
    private String seatStatus;

    @Schema(description = "좌석을 잠근 사용자 ID", example = "park", nullable = true)
    private Long lockedUserId;

    @Schema(description = "좌석 잠금 시각", example = "2025-11-02 13:30:00", nullable = true)
    private String lockedAt;
}

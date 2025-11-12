package kr.hhplus.be.server.concert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConcertReserveRequestDto {

    @Schema(description = "concertId", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "필수 입력 값입니다.")
    private Long concertId;

    @Schema(description = "scheduleId", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "필수 입력 값입니다.")
    private Long scheduleId;

    @Schema(description = "seatId", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "필수 입력 값입니다.")
    private Long seatId;

    @Schema(description = "userId", example = "park", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "는 필수 입력 값입니다.")
    private String userId;
}

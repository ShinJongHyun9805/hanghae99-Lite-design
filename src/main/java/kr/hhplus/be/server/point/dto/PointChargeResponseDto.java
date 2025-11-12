package kr.hhplus.be.server.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "포인트 충전 응답 DTO")
public class PointChargeResponseDto {

    @Schema(description = "포인트 ID", example = "1")
    private Long id;

    @Schema(description = "회원 ID", example = "park")
    private String memberId;

    @Schema(description = "충전 후 잔액", example = "10")
    private int pointAmt;
}

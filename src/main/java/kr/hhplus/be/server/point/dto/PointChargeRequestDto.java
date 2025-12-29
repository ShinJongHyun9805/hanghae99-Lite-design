package kr.hhplus.be.server.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PointChargeRequestDto {

    @Schema(description = "회원 ID", example = "park", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "회원 ID는 필수 입력 값입니다.")
    String memberId ;

    @Schema(description = "충전 금액 (1원 이상)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "충전 금액은 1원 이상이어야 합니다.")
    Integer amount;
}

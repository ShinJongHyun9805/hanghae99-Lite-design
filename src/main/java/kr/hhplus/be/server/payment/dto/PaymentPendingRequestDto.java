package kr.hhplus.be.server.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "결제 대기 응답 DTO")
public class PaymentPendingRequestDto {

    @Schema(description = "결제 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 1, message = "결제 ID는 필수 입력 값입니다.")
    Integer amount;

    @Schema(description = "회원 ID", example = "park", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "회원 ID는 필수 입력 값입니다.")
    String memberId;

}

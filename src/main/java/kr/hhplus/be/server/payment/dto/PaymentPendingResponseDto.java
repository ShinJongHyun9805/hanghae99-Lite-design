package kr.hhplus.be.server.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "결제 대기 응답 DTO")
public class PaymentPendingResponseDto {

    @Schema(description = "결제 ID", example = "1")
    private Long paymentId;

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "회원명", example = "jonghyun")
    private String memberName;

    @Schema(description = "결제 상태", example = "결제 대기")
    private String paymentStatusNm;


    @Schema(description = "콘서트명", example = "wanna see you dance again")
    private String title;

    @Schema(description = "결제 금액", example = "100,000")
    private int price;

    @Schema(description = "콘서트 날짜 ", example = "2025-12-03")
    private String startDate;

    @Schema(description = "좌석 번호", example = "1")
    private int seatNumber;

    @Schema(description = "결제 완료 날짜 ", example = "2025-11-03")
    private String paymentDate;

    @Schema(description = "결제 취소 사유 ", example = "결제 시간 초과로 인한 자동 취소")
    private String cancelReason;

}

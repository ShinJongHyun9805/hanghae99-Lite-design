package kr.hhplus.be.server.payment.result;

import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentHistoryResult {

    private Long paymentId;

    private Long concertId;

    private Long concertScheduleId;

    private Long seatId;

    private Long memberId;

    private int price;

    private PaymentStatus paymentStatus;

    private LocalDateTime paymentAt;

    private String cancelReason;

    private SeatStatus seatStatus;

    private LocalDateTime lockedAt;

    private String title;

    private String venueName;

    private LocalDateTime regDt;

    private LocalDateTime modDt;

    @Override
    public String toString() {
        return "PaymentHistoryResult{" +
                "paymentId=" + paymentId +
                ", concertId=" + concertId +
                ", concertScheduleId=" + concertScheduleId +
                ", seatId=" + seatId +
                ", memberId=" + memberId +
                ", price=" + price +
                ", paymentStatus=" + paymentStatus +
                ", paymentAt=" + paymentAt +
                ", cancelReason='" + cancelReason + '\'' +
                ", seatStatus=" + seatStatus +
                ", lockedAt=" + lockedAt +
                ", title='" + title + '\'' +
                ", venueName='" + venueName + '\'' +
                ", regDt=" + regDt +
                ", modDt=" + modDt +
                '}';
    }
}

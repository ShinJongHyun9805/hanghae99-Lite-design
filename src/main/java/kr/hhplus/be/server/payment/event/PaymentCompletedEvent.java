package kr.hhplus.be.server.payment.event;

import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long concertScheduleId,
        LocalDateTime paymentAt
) {
}

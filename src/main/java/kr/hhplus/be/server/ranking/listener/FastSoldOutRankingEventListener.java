package kr.hhplus.be.server.ranking.listener;

import kr.hhplus.be.server.payment.event.PaymentCompletedEvent;
import kr.hhplus.be.server.ranking.service.FastSoldOutRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastSoldOutRankingEventListener {

    private final FastSoldOutRedisService fastSoldOutRedisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        if (event.concertScheduleId() == null || event.paymentAt() == null) {
            log.warn("FastSoldOut event missing data. paymentId={}, scheduleId={}",
                    event.paymentId(),
                    event.concertScheduleId()
            );
            return;
        }

        try {
            fastSoldOutRedisService.recordPayment(
                    event.concertScheduleId(),
                    event.paymentAt()
            );
        } catch (Exception e) {
            log.error("FastSoldOut redis update failed. paymentId={}, scheduleId={}",
                    event.paymentId(),
                    event.concertScheduleId(),
                    e
            );
        }
    }
}

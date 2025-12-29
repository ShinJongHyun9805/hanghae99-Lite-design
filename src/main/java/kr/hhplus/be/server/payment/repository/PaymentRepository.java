package kr.hhplus.be.server.payment.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.result.PaymentHistoryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            select new kr.hhplus.be.server.payment.result.PaymentHistoryResult(
                p.Id
                , p.concertId
                , p.concertScheduleId
                , p.seatId
                , p.memberId
                , p.price
                , p.paymentStatus
                , p.paymentAt
                , p.cancelReason
                , s.seatStatus
                , s.lockedAt
                , c.title
                , c.venueName
                , p.regDt
                , p.modDt
            )
              from Payment p
              join Seat s on s.seatId = p.seatId
              join Concert c ON c.id = p.concertId
             where p.Id = (
                           select max(p2.Id)
                             from Payment p2
                            where p2.seatId = p.seatId
                              and p2.memberId = :memberId
                          )
            """)
    List<PaymentHistoryResult> findAllHistoryByMemberId(@Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.Id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @Modifying
    @Query("""
        update Payment p
           set p.paymentStatus = kr.hhplus.be.server.payment.domain.PaymentStatus.CANCEL,
               p.cancelReason = :reason,
               p.modDt = CURRENT_TIMESTAMP
         where p.seatId = :seatId
           and p.Id <> :currentPaymentId
           and p.paymentStatus = kr.hhplus.be.server.payment.domain.PaymentStatus.PENDING
    """)
    void cancelOtherPendings(
            @Param("seatId") Long seatId,
            @Param("currentPaymentId") Long currentPaymentId,
            @Param("reason") String reason
    );
}

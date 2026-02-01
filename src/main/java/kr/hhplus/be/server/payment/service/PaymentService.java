package kr.hhplus.be.server.payment.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exception.PaymentException;
import kr.hhplus.be.server.common.exception.SeatException;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.dto.PaymentDto.PaymentAllListResponse;
import kr.hhplus.be.server.payment.dto.PaymentDto.PaymentCompleteResponse;
import kr.hhplus.be.server.payment.dto.PaymentDto.PaymentListResult;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.payment.result.PaymentHistoryResult;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.repository.PointRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;
    private final PointRepository pointRepository;

    public PaymentAllListResponse getPaymentHistory(Long memberId) {

        List<PaymentHistoryResult> paymentsHistoryList = paymentRepository.findAllHistoryByMemberId(memberId);

        // 현재 시간
        LocalDateTime now = LocalDateTime.now();

        // 결제 대기
        List<PaymentListResult> pendingPaymentList = paymentsHistoryList.stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PENDING)
                .filter(e -> ObjectUtils.isNotEmpty(e.getLockedAt()))
                .filter(e -> e.getLockedAt().plusMinutes(5).isAfter(now) || e.getLockedAt().plusMinutes(5).isEqual(now))
                .map(e -> new PaymentListResult(
                            e.getPaymentId()
                            , e.getTitle()
                            , e.getVenueName()
                            , e.getPrice()
                            , e.getPaymentStatus().getDisplayName()
                            , e.getModDt()
                    ))
                .toList();

        // 결제 완료
        // 결제 완료
        List<PaymentListResult> paymentCompletedList = paymentsHistoryList.stream()
                        .filter(e -> e.getPaymentStatus() == PaymentStatus.PAYMENT)
                        .map(e -> new PaymentListResult(
                                e.getPaymentId(),
                                e.getTitle(),
                                e.getVenueName(),
                                e.getPrice(),
                                e.getPaymentStatus().getDisplayName(),
                                e.getModDt()
                        ))
                        .toList();

        // 결제 취소
        List<PaymentListResult> paymentCancelList = paymentsHistoryList.stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.CANCEL)
                .map(e -> new PaymentListResult(
                            e.getPaymentId()
                            , e.getTitle()
                            , e.getVenueName()
                            , e.getPrice()
                            , e.getPaymentStatus().getDisplayName()
                            , e.getModDt()
                    ))
                .toList();

        return new PaymentAllListResponse(pendingPaymentList, paymentCompletedList, paymentCancelList);
    }

    @Transactional
    public PaymentCompleteResponse completePayment(Long paymentId) {

        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new PaymentException().InvalidPaymentException());

        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new PaymentException().InvalidPaymentException();
        }

        Seat seat = seatRepository.findByIdForUpdate(payment.getSeatId())
                .orElseThrow(() -> new SeatException().InvalidSeatException());

        Concert concert = concertRepository.findById(payment.getConcertId())
                .orElseThrow(() -> new PaymentException().InvalidPaymentException());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = seat.getLockedAt().plusMinutes(5);

        boolean expired = expireAt.isBefore(now);

        if (expired) {
            payment.setPaymentStatus(PaymentStatus.CANCEL);
            payment.setCancelReason("LOCK_EXPIRED");

            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockedUserId(null);
        } else {
            // 포인트 확인 및 차감 (동시성 고려를 위해 SELECT FOR UPDATE 사용)
            String memberIdStr = String.valueOf(payment.getMemberId());
            Point point = pointRepository.findForUpdateByMemberId(memberIdStr)
                    .orElseGet(() -> {
                        Point newPoint = new Point();
                        newPoint.setMemberId(memberIdStr);
                        newPoint.setPointAmt(0);
                        return pointRepository.save(newPoint);
                    });

            int requiredAmount = payment.getPrice();
            int currentPoint = point.getPointAmt();

            // 포인트가 부족한 경우 결제 취소
            if (currentPoint < requiredAmount) {
                payment.setPaymentStatus(PaymentStatus.CANCEL);
                payment.setCancelReason("INSUFFICIENT_POINT");

                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedAt(null);
                seat.setLockedUserId(null);
            } else {
                // 포인트 차감
                point.usePoint(requiredAmount);

                payment.setPaymentStatus(PaymentStatus.PAYMENT);
                payment.setPaymentAt(now);

                seat.setSeatStatus(SeatStatus.RESERVED);

                paymentRepository.cancelOtherPendings(
                        payment.getSeatId(),
                        payment.getId(),
                        "요청 시간 만료"
                );
            }
        }

        return new PaymentCompleteResponse(paymentId
                , concert.getTitle()
                , concert.getVenueName()
                , payment.getPrice()
                , payment.getPaymentStatus().getDisplayName()
                , payment.getPaymentAt()
        );
    }
}

package kr.hhplus.be.server_v2.usecase.lockseat;


import kr.hhplus.be.server.common.exception.ConcertScheduleException;
import kr.hhplus.be.server.common.exception.SeatException;
import kr.hhplus.be.server_v2.adapters.gateway.ConcertScheduleRepositoryPort;
import kr.hhplus.be.server_v2.adapters.gateway.MemberRepositoryPort;
import kr.hhplus.be.server_v2.adapters.gateway.PaymentRepositoryPort;
import kr.hhplus.be.server_v2.adapters.gateway.SeatRepositoryPort;
import kr.hhplus.be.server_v2.entity.concert_schedule.ConcertSchedule;
import kr.hhplus.be.server_v2.entity.member.Member;
import kr.hhplus.be.server_v2.entity.payment.Payment;
import kr.hhplus.be.server_v2.entity.payment.PaymentStatus;
import kr.hhplus.be.server_v2.entity.seat.Seat;
import kr.hhplus.be.server_v2.entity.seat.SeatStatus;
import kr.hhplus.be.server_v2.usecase.lockseat.LockSeatOutputPort.LockSeatResult;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class LockSeatInteractor implements LockSeatInputPort {

    private final SeatRepositoryPort seatRepo;
    private final MemberRepositoryPort memberRepo;
    private final ConcertScheduleRepositoryPort scheduleRepo;
    private final PaymentRepositoryPort paymentRepo;
    private final LockSeatOutputPort presenter;

    @Override
    public void lockSeat(LockSeatCommand command) {

        Member member = memberRepo.findByMemberId(command.memberId())
                .orElseThrow();

        Seat seat = seatRepo.findSeatForLock(command.seatId(), command.concertScheduleId())
                .orElseThrow(() -> new SeatException().NotExistsSeatException());

        if (seat.getSeatStatus() == SeatStatus.RESERVED) {
            throw new SeatException().ReservedSeatException();
        }

        // 예약된 좌석 > 만료 여부 확인
        if (seat.getSeatStatus() == SeatStatus.LOCKED && ObjectUtils.isNotEmpty(seat.getLockedAt())) {
            LocalDateTime now = LocalDateTime.now();
            boolean invalidLocked = seat.getLockedAt().plusMinutes(5).isAfter(now);

            if (invalidLocked) {
                throw new SeatException().ReservedSeatException();
            }

        }

        // 해당 좌석 LOCKED 처리
        seat.setSeatId(command.seatId());
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedUserId(member.getId());
        seat.setLockedAt(LocalDateTime.now());

        seatRepo.save(seat);

        // 결제 대기 요청
        ConcertSchedule concertSchedule = scheduleRepo.findById(command.concertScheduleId())
                .orElseThrow(() -> new ConcertScheduleException("유효하지 않은 콘서트 일정입니다."));

        Payment payment = new Payment();
        payment.setConcertId(concertSchedule.getConcert().getId());
        payment.setConcertScheduleId(command.concertScheduleId());
        payment.setSeatId(command.seatId());
        payment.setMemberId(member.getId());
        payment.setPrice(concertSchedule.getConcert().getPrice());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentAt(null);
        payment.setCancelReason(null);
        payment.setRegDt(LocalDateTime.now());
        payment.setModDt(LocalDateTime.now());

        paymentRepo.save(payment);

        presenter.success(new LockSeatResult(
                seat.getSeatId(),
                command.concertScheduleId(),
                member.getId()
        ));
    }
}

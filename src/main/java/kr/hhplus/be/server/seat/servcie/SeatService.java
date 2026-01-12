package kr.hhplus.be.server.seat.servcie;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exception.ConcertScheduleException;
import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.common.exception.SeatException;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.queue.service.QueueTokenService;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.dto.SeatDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto.SeatResponse;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final ConcertScheduleRepository concertScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final QueueTokenService queueTokenService;

    public seatResponseDto getSeat(Long concertScheduleId) {

        List<SeatResponse> availableSeatList = seatRepository.findBySchedule_ConcertScheduleIdOrderBySeatNumberAsc(concertScheduleId)
                .stream()
                .peek(seat -> {
                    if (seat.getSeatStatus() == SeatStatus.LOCKED && seat.getLockedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                        seat.setSeatStatus(SeatStatus.AVAILABLE);
                        seat.setLockedUserId(null);
                        seat.setLockedAt(null);
                    }
                })
                .filter(seat -> seat.getSeatStatus() == SeatStatus.AVAILABLE)
                .map(e -> {
                    SeatResponse seat = new SeatResponse();
                    seat.setSeatId(e.getSeatId());
                    seat.setConcertScheduleId(e.getSchedule().getConcertScheduleId());

                    return seat;
                })
                .toList();

        return new seatResponseDto(availableSeatList);
    }

    @Transactional
    public void seatReservationRequest(SeatDto.seatReservationRequestDto requestDto, String queueToken, UserDetails userDetails) {

        queueTokenService.validateActiveToken(queueToken, userDetails.getUsername());

        Member member = memberRepository.findByMemberId(userDetails.getUsername())
                .orElseThrow(InvalidUserException::new);

        Seat seat = seatRepository.findBySeatIdAndSchedule_ConcertScheduleId(requestDto.seatId(), requestDto.concertScheduleId())
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
        seat.setSeatId(requestDto.seatId());
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedUserId(member.getId());
        seat.setLockedAt(LocalDateTime.now());

        seatRepository.save(seat);

        // 결제 대기 요청
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(requestDto.concertScheduleId())
                .orElseThrow(() -> new ConcertScheduleException("유효하지 않은 콘서트 일정입니다."));

        Payment payment = new Payment();
        payment.setConcertId(concertSchedule.getConcert().getId());
        payment.setConcertScheduleId(requestDto.concertScheduleId());
        payment.setSeatId(requestDto.seatId());
        payment.setMemberId(member.getId());
        payment.setPrice(concertSchedule.getConcert().getPrice());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentAt(null);
        payment.setCancelReason(null);
        payment.setRegDt(LocalDateTime.now());
        payment.setModDt(LocalDateTime.now());

        paymentRepository.save(payment);
    }


}

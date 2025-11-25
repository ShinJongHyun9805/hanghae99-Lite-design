package kr.hhplus.be.server.seat;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto.SeatResponse;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@SpringBootTest
class SeatServiceTest {

    @Autowired private MemberRepository memberRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ConcertRepository concertRepository;
    @Autowired private ConcertScheduleRepository concertScheduleRepository;
    @Autowired private PaymentRepository paymentRepository;

    @Test
    void 예약가능한_좌석_조회_response() throws Exception {

        List<SeatResponse> result = seatRepository.findBySchedule_ConcertScheduleIdOrderBySeatNumberAsc(1L)
                .stream()
                .map(e -> {
                    SeatResponse seat = new SeatResponse();
                    seat.setSeatId(e.getSeatId());
                    seat.setConcertScheduleId(e.getSchedule().getConcertScheduleId());

                    return seat;
                })
                .toList();

        Assertions.assertTrue(!result.isEmpty());
    }

    @Test
    @Transactional
    void 예약_가능한_좌석_조회_배정_해제() {

        Long concertScheduleId = 1L;

        ConcertSchedule schedule = new ConcertSchedule();
        schedule.setConcertScheduleId(concertScheduleId);

        Long seatId = 1L;

        Seat mockSeat1 = new Seat();
        mockSeat1.setSeatId(seatId);
        mockSeat1.setSeatNumber(51);
        mockSeat1.setSeatStatus(SeatStatus.LOCKED);
        mockSeat1.setLockedUserId(null);
        mockSeat1.setLockedAt(LocalDateTime.of(2025, 11, 24, 22, 05));
        mockSeat1.setSchedule(schedule);

        Long seatId2 = 2L;

        Seat mockSeat2 = new Seat();
        mockSeat2.setSeatId(seatId2);
        mockSeat2.setSeatNumber(52);
        mockSeat2.setSeatStatus(SeatStatus.LOCKED);
        mockSeat2.setLockedUserId(10L);
        mockSeat2.setLockedAt(LocalDateTime.of(2025, 11, 24, 22, 05));
        mockSeat2.setSchedule(schedule);

        Long seatId3 = 3L;

        Seat mockSeat3 = new Seat();
        mockSeat3.setSeatId(seatId3);
        mockSeat3.setSeatNumber(52);
        mockSeat3.setSeatStatus(SeatStatus.AVAILABLE);
        mockSeat3.setLockedUserId(null);
        mockSeat3.setLockedAt(null);
        mockSeat3.setSchedule(schedule);

        List<Seat> seatList = List.of(mockSeat1, mockSeat2, mockSeat3);

        List<Seat> filterSeatList = seatList.stream()
                .peek(seat -> {
                    if (seat.getSeatStatus() == SeatStatus.LOCKED && seat.getLockedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                        seat.setSeatStatus(SeatStatus.AVAILABLE);
                        seat.setLockedUserId(null);
                        seat.setLockedAt(null);
                    }
                })
                .filter(seat -> seat.getSeatStatus() == SeatStatus.AVAILABLE)
                .toList();

        Assertions.assertEquals(filterSeatList.size(), 3);
    }


    @Test
    @Transactional
    void 좌석_예약_요청() {

        Long concertScheduleId = 2L;
        Long seatId = 51L;

        Member member = memberRepository.findByMemberId(userDetails.getUsername()).get();

        Seat seat = seatRepository.findBySeatIdAndSchedule_ConcertScheduleId(seatId, concertScheduleId).get();
        seat.setSeatId(seatId);
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedUserId(member.getId());
        seat.setLockedAt(LocalDateTime.now());

        seatRepository.save(seat);

        Seat seatResult = seatRepository.findById(seatId).get();

        Assertions.assertEquals(seatResult.getSeatStatus(), SeatStatus.LOCKED);
        Assertions.assertEquals(seatResult.getLockedUserId(), member.getId());
    }

    @Test
    @Transactional
    void 결제_대기() {

        Long concertScheduleId = 2L;
        Long seatId = 51L;

        Member member = memberRepository.findByMemberId(userDetails.getUsername()).get();
        Seat seat = seatRepository.findBySeatIdAndSchedule_ConcertScheduleId(seatId, concertScheduleId).get();
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(concertScheduleId).get();

        Payment payment = new Payment();
        payment.setConcertId(concertSchedule.getConcert().getId());
        payment.setConcertScheduleId(concertScheduleId);
        payment.setSeatId(seatId);
        payment.setMemberId(member.getId());
        payment.setPrice(concertSchedule.getConcert().getPrice());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentAt(LocalDateTime.now());
        payment.setCancelReason(null);

        Payment save = paymentRepository.save(payment);

        Payment paymentResult = paymentRepository.findById(save.getId()).get();

        Assertions.assertEquals(paymentResult.getPaymentStatus(), PaymentStatus.PENDING);
        Assertions.assertEquals(paymentResult.getMemberId(), member.getId());

    }

    UserDetails userDetails = new UserDetails() {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return "park";
        }
    };
}
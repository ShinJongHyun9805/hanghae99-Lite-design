package kr.hhplus.be.server.payment.service;

import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.dto.PaymentDto.PaymentListResult;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.payment.result.PaymentHistoryResult;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    SeatRepository seatRepository;

    @Mock
    ConcertRepository concertRepository;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void 해당_유저의_결제_대기_목록_조회() {

        Long memberId = 1L;

        PaymentHistoryResult res1 = new PaymentHistoryResult(2L
                , 1L
                , 2L
                , 100L
                , 1L
                , 15000
                , PaymentStatus.PENDING
                , null
                , null
                , SeatStatus.LOCKED
                , LocalDateTime.of(2025, 11, 30, 15, 53)
                , "title"
                , "test 콘서트 장"
                , LocalDateTime.now()
                , LocalDateTime.now()
        );

        PaymentHistoryResult res2 = new PaymentHistoryResult(3L
                , 1L
                , 2L
                , 99L
                , 1L
                , 15000
                , PaymentStatus.PENDING
                , null
                , null
                , SeatStatus.LOCKED
                , LocalDateTime.of(2025, 11, 25, 15, 53)
                , "title"
                , "test 콘서트 장"
                , LocalDateTime.now()
                , LocalDateTime.now()
        );

        List<PaymentHistoryResult> list = List.of(res1, res2);

        PaymentRepository repository = mock(PaymentRepository.class);
        when(repository.findAllHistoryByMemberId(memberId)).thenReturn(list);

        List<PaymentHistoryResult> pendingPaymentsByMemberId = repository.findAllHistoryByMemberId(memberId);

        LocalDateTime now = LocalDateTime.now();
        List<PaymentListResult> filterPaymentList = pendingPaymentsByMemberId.stream()
                .filter(e -> e.getSeatStatus() == SeatStatus.LOCKED)
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PENDING)
                .filter(e -> ObjectUtils.isNotEmpty(e.getLockedAt()))
                .filter(e -> e.getLockedAt().plusMinutes(5).isAfter(now) || e.getLockedAt().plusMinutes(5).isEqual(now))
                .map(e -> {
                    return new PaymentListResult(e.getPaymentId()
                            , e.getTitle()
                            , e.getVenueName()
                            , e.getPrice()
                            , e.getPaymentStatus().getDisplayName()
                            , e.getModDt()
                    );
                })
                .toList();


        assertEquals(filterPaymentList.size(), 1);
        Assertions.assertTrue(!filterPaymentList.isEmpty());
    }

    @Test
    void 결제_성공하면_Payment_STATUS가_PAYMENT로_Seat_STATUS가_RESERVED가_된다() {

        Payment payment = new Payment();
        payment.setId(12L);
        payment.setConcertId(1L);
        payment.setConcertScheduleId(2L);
        payment.setPaymentAt(null);
        payment.setSeatId(99L);
        payment.setCancelReason(null);
        payment.setMemberId(1L);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPrice(150000);

        Seat seat = new Seat();
        seat.setSeatId(99L);
        seat.setSeatNumber(49);
        seat.setLockedUserId(1L);
        seat.setLockedAt(LocalDateTime.of(2025, 12, 29, 17, 00, 50));
        seat.setSeatStatus(SeatStatus.LOCKED);

        Concert concert = new Concert();
        concert.setId(1L);
        concert.setTitle("test");
        concert.setVenueName("testName");
        concert.setPrice(150000);

        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(seatRepository.findByIdForUpdate(seat.getSeatId())).thenReturn(Optional.of(seat));
        when(concertRepository.findById(payment.getConcertId())).thenReturn(Optional.of(concert));

        // When
        paymentService.completePayment(payment.getId());

        // Then
        assertEquals(PaymentStatus.PAYMENT, payment.getPaymentStatus());
        assertEquals(SeatStatus.RESERVED, seat.getSeatStatus());
        assertNotNull(payment.getPaymentAt());
    }

    @Test
    void 좌석_LOCK이_만료되면_결제는_CANCEL되고_좌석은_AVAILABLE로_변경된다() {

        Payment payment = new Payment();
        payment.setId(12L);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setSeatId(99L);

        Seat seat = new Seat();
        seat.setSeatId(99L);
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedAt(LocalDateTime.now().minusMinutes(10)); // 만료됨

        Concert concert = new Concert();
        concert.setId(1L);
        concert.setTitle("test");
        concert.setVenueName("testName");
        concert.setPrice(150000);

        when(paymentRepository.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(seatRepository.findByIdForUpdate(seat.getSeatId())).thenReturn(Optional.of(seat));
        when(concertRepository.findById(payment.getConcertId())).thenReturn(Optional.of(concert));

        // When
        paymentService.completePayment(payment.getId());

        // Then
        assertEquals(PaymentStatus.CANCEL, payment.getPaymentStatus());
        assertEquals("LOCK_EXPIRED", payment.getCancelReason());
        assertEquals(SeatStatus.AVAILABLE, seat.getSeatStatus());
        assertNull(seat.getLockedUserId());
        assertNull(seat.getLockedAt());
    }
}
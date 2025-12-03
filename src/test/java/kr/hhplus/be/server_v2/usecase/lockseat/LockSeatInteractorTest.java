package kr.hhplus.be.server_v2.usecase.lockseat;

import kr.hhplus.be.server.common.exception.SeatException;
import kr.hhplus.be.server_v2.adapters.gateway.ConcertScheduleRepositoryPort;
import kr.hhplus.be.server_v2.adapters.gateway.MemberRepositoryPort;
import kr.hhplus.be.server_v2.adapters.gateway.PaymentRepositoryPort;
import kr.hhplus.be.server_v2.adapters.gateway.SeatRepositoryPort;
import kr.hhplus.be.server_v2.adapters.web.LockSeatPresenter;
import kr.hhplus.be.server_v2.entity.concert.Concert;
import kr.hhplus.be.server_v2.entity.concert_schedule.ConcertSchedule;
import kr.hhplus.be.server_v2.entity.member.Member;
import kr.hhplus.be.server_v2.entity.payment.Payment;
import kr.hhplus.be.server_v2.entity.seat.Seat;
import kr.hhplus.be.server_v2.entity.seat.SeatStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;


class LockSeatInteractorTest {

    private SeatRepositoryPort seatRepo;
    private MemberRepositoryPort memberRepo;
    private ConcertScheduleRepositoryPort scheduleRepo;
    private PaymentRepositoryPort paymentRepo;
    private LockSeatPresenter presenter;

    private LockSeatInteractor interactor;

    @BeforeEach
    void setup() {
        seatRepo = mock(SeatRepositoryPort.class);
        memberRepo = mock(MemberRepositoryPort.class);
        scheduleRepo = mock(ConcertScheduleRepositoryPort.class);
        paymentRepo = mock(PaymentRepositoryPort.class);
        presenter = new LockSeatPresenter();

        interactor = new LockSeatInteractor(
                seatRepo,
                memberRepo,
                scheduleRepo,
                paymentRepo,
                presenter
        );
    }

    @Test
    void 좌석_LOCK_정상_성공() {

        // GIVEN
        String memberId = "park";
        Long scheduleId = 100L;
        Long seatId = 5L;

        // --- Member ---
        Member member = Member.builder()
                .id(1L)
                .memberId(memberId)
                .memberName("박유저")
                .build();

        when(memberRepo.findByMemberId(memberId))
                .thenReturn(Optional.of(member));

        // --- Seat ---
        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setSeatNumber(10);
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat.setLockedAt(null);

        when(seatRepo.findSeatForLock(seatId, scheduleId))
                .thenReturn(Optional.of(seat));

        when(seatRepo.save(any(Seat.class)))
                .thenReturn(seat);

        // --- Schedule + Concert ---
        Concert concert = new Concert();
        concert.setId(999L);
        concert.setPrice(150000);

        ConcertSchedule schedule = new ConcertSchedule();
        schedule.setConcertScheduleId(scheduleId);
        schedule.setConcert(concert);

        when(scheduleRepo.findById(scheduleId))
                .thenReturn(Optional.of(schedule));

        when(paymentRepo.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        interactor.lockSeat(new LockSeatInputPort.LockSeatCommand(
                scheduleId,
                seatId,
                memberId
        ));

        // THEN: Presenter 성공 체크
        assertThat(presenter.getError()).isNull();
        assertThat(presenter.getResult()).isNotNull();

        LockSeatOutputPort.LockSeatResult res = presenter.getResult();
        assertThat(res.seatId()).isEqualTo(seatId);
        assertThat(res.concertScheduleId()).isEqualTo(scheduleId);
        assertThat(res.memberId()).isEqualTo(member.getId());

        // 좌석 상태 체크
        assertThat(seat.getSeatStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(seat.getLockedUserId()).isEqualTo(1L);
        assertThat(seat.getLockedAt()).isNotNull();

        // Payment 저장 확인
        verify(paymentRepo, times(1)).save(any(Payment.class));
    }


    @Test
    void 이미_LOCK_중이면_예약실패() {

        // GIVEN
        String memberId = "kim";
        Long scheduleId = 10L;
        Long seatId = 3L;

        Member member = Member.builder()
                .id(2L)
                .memberId(memberId)
                .build();

        when(memberRepo.findByMemberId(memberId))
                .thenReturn(Optional.of(member));

        // seat locked 3분 전 → 아직 유효
        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedUserId(999L);
        seat.setLockedAt(LocalDateTime.now().minusMinutes(3));

        when(seatRepo.findSeatForLock(seatId, scheduleId))
                .thenReturn(Optional.of(seat));

        // WHEN
        assertThrows(SeatException.class, () -> {
            interactor.lockSeat(new LockSeatInputPort.LockSeatCommand(
                    scheduleId,
                    seatId,
                    memberId
            ));
        });

        // Payment 저장되면 안 됨
        verify(paymentRepo, never()).save(any());
    }
}
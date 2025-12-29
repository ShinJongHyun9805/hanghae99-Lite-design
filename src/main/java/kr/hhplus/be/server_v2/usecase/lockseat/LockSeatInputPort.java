package kr.hhplus.be.server_v2.usecase.lockseat;

public interface LockSeatInputPort {

    void lockSeat(LockSeatCommand command);

    record LockSeatCommand(
            Long concertScheduleId,
            Long seatId,
            String memberId
    ) {}
}

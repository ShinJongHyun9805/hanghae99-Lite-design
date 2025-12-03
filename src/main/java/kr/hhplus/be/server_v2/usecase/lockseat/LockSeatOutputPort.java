package kr.hhplus.be.server_v2.usecase.lockseat;

public interface LockSeatOutputPort{

    void success(LockSeatResult result);
    void fail(String message);

    record LockSeatResult(
            Long seatId,
            Long concertScheduleId,
            Long memberId
    ) {}
}

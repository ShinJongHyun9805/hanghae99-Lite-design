package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.seat.Seat;

import java.util.Optional;

public interface SeatRepositoryPort {

    Optional<Seat> findSeatForLock(Long seatId, Long scheduleId);

    Seat save(Seat seat);
}

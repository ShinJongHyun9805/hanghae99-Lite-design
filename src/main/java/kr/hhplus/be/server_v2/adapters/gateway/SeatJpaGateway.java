package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.seat.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SeatJpaGateway implements SeatRepositoryPort {

    private final SeatRepository repository;

    @Override
    public Optional<Seat> findSeatForLock(Long seatId, Long scheduleId) {
        return repository.findSeatForLock(seatId, scheduleId);
    }

    @Override
    public Seat save(Seat seat) {
        return repository.save(seat);
    }
}

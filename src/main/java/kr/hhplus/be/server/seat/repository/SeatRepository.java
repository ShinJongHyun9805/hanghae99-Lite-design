package kr.hhplus.be.server.seat.repository;

import kr.hhplus.be.server.seat.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySchedule_ConcertScheduleIdOrderBySeatNumberAsc(Long concertScheduleId);

    /**
     * DB 비관적 락 제거 - Redis 분산락으로 대체
     */
    Optional<Seat> findBySeatIdAndSchedule_ConcertScheduleId(Long seatId, Long concertScheduleId);
}

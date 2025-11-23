package kr.hhplus.be.server.seat.repository;

import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySchedule_ConcertScheduleIdAndSeatStatusOrderBySeatNumberAsc(Long concertScheduleId, SeatStatus seatStatus);
}

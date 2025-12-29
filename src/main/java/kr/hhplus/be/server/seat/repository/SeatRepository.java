package kr.hhplus.be.server.seat.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.seat.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySchedule_ConcertScheduleIdOrderBySeatNumberAsc(Long concertScheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Seat> findBySeatIdAndSchedule_ConcertScheduleId(Long seatId, Long concertScheduleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.seatId = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") Long seatId);
}

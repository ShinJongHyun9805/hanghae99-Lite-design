package kr.hhplus.be.server_v2.adapters.gateway;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server_v2.entity.seat.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
          from Seat s
         where s.seatId = :seatId
           and s.schedule.concertScheduleId = :scheduleId
    """)
    Optional<Seat> findSeatForLock(
            @Param("seatId") Long seatId,
            @Param("scheduleId") Long scheduleId
    );
}

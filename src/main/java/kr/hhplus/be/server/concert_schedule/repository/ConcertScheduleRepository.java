package kr.hhplus.be.server.concert_schedule.repository;

import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {

    @Query("""
            select cs
              from ConcertSchedule cs
              join fetch cs.concert c
             where cs.concertScheduleId in :scheduleIds
            """)
    List<ConcertSchedule> findAllWithConcertByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
}

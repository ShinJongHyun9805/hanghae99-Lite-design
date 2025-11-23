package kr.hhplus.be.server.concert_schedule.repository;

import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertScheduleRepository extends JpaRepository<ConcertSchedule, Long> {
}

package kr.hhplus.be.server.concert.repository;

import kr.hhplus.be.server.concert.domain.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    @Query("select c from Concert c join fetch c.schedules")
    List<Concert> findAllWithConcertSchedules();

    Optional<Concert> findBySchedules_ConcertScheduleId(Long concertScheduleId);
}

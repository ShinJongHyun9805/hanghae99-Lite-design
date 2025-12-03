package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.concert_schedule.ConcertSchedule;

import java.util.Optional;

public interface ConcertScheduleRepository {

    Optional<ConcertSchedule> findById(Long concertScheduledId);
}

package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.concert_schedule.ConcertSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConcertScheduleJpaGateway implements ConcertScheduleRepositoryPort {

    private final ConcertScheduleRepository repository;

    @Override
    public Optional<ConcertSchedule> findById(Long id) {
        return repository.findById(id);
    }
}

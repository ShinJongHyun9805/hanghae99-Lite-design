package kr.hhplus.be.server.concert.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.dto.ConcertDto.ConcertListResponse;
import kr.hhplus.be.server.concert.dto.ConcertDto.ConcertListResponse.ConcertScheduleResult;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    @Transactional
    public List<ConcertListResponse> getConcertList() {

        List<Concert> concertList = concertRepository.findAllWithConcertSchedules();

        return concertList.stream()
                .map(c -> new ConcertListResponse(
                        c.getId(),
                        c.getTitle(),
                        c.getVenueName(),
                        c.getCreated_dt(),
                        c.getPrice(),
                        c.getOpenYn(),
                        c.getSchedules().stream()
                                .map(e -> {
                                    ConcertScheduleResult concertSchedule = new ConcertScheduleResult();
                                    concertSchedule.setConcertScheduleId(e.getConcertScheduleId());
                                    concertSchedule.setConcertDate(e.getConcertDate());

                                    return concertSchedule;
                                })
                                .toList()
                ))
                .toList();
    }
}

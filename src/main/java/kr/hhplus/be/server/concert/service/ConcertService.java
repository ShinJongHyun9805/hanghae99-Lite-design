package kr.hhplus.be.server.concert.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.config.cache.CacheConfig;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.dto.ConcertDto.ConcertListResponse;
import kr.hhplus.be.server.concert.dto.ConcertDto.ConcertListResponse.ConcertScheduleResult;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertService {

    private final ConcertRepository concertRepository;

    /**
     * 콘서트 목록 + 스케줄 목록 조회 (Redis 캐싱 적용)
     *
     * [캐시 전략]
     * - 캐시명: concertList
     * - key: 'all' (전체 목록 단일 키)
     * - TTL: 5분(CacheConfig)
     * - sync=true: 캐시 미스 순간 동일 인스턴스 내 DB 중복 조회 완화
     * - unless: 빈 결과는 캐싱하지 않음
     */
    @Transactional
    @Cacheable(
            value = CacheConfig.CONCERT_LIST_CACHE,
            key = "'all'",
            sync = true
    )
    public List<ConcertListResponse> getConcertList() {

        // join fetch로 N+1 문제 방지
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

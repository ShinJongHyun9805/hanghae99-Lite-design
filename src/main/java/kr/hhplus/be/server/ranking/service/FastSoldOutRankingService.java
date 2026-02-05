package kr.hhplus.be.server.ranking.service;

import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.ranking.dto.FastSoldOutRankingDto.FastSoldOutRankingResponse;
import kr.hhplus.be.server.ranking.redis.FastSoldOutRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class FastSoldOutRankingService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate stringRedisTemplate;
    private final ConcertScheduleRepository concertScheduleRepository;

    public List<FastSoldOutRankingResponse> getDailyRanking(String date, int limit) {
        LocalDate targetDate = parseDate(date);
        if (limit <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "limit must be positive");
        }

        String rankKey = FastSoldOutRedisKeys.rankDay(DAY_FORMATTER.format(targetDate));
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(rankKey, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }

        List<Long> scheduleIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .toList();

        if (scheduleIds.isEmpty()) {
            return List.of();
        }

        List<ConcertSchedule> schedules = concertScheduleRepository.findAllWithConcertByScheduleIds(scheduleIds);
        Map<Long, ConcertSchedule> scheduleMap = new HashMap<>();
        for (ConcertSchedule schedule : schedules) {
            scheduleMap.put(schedule.getConcertScheduleId(), schedule);
        }

        List<FastSoldOutRankingResponse> responses = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }

            Long scheduleId = Long.valueOf(tuple.getValue());
            ConcertSchedule schedule = scheduleMap.get(scheduleId);
            if (schedule == null) {
                continue;
            }

            responses.add(new FastSoldOutRankingResponse(
                    scheduleId,
                    tuple.getScore().longValue(),
                    schedule.getConcert().getTitle(),
                    schedule.getConcert().getVenueName(),
                    schedule.getConcertDate()
            ));
        }

        return responses;
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DAY_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(BAD_REQUEST, "date must be YYYYMMDD", e);
        }
    }
}

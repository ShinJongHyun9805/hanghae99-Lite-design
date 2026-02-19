package kr.hhplus.be.server.ranking.service;

import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.ranking.redis.FastSoldOutRedisKeys;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastSoldOutRedisService {

    private static final Duration SOLD_TTL = Duration.ofDays(30);
    private static final Duration RANK_TTL = Duration.ofDays(30);
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private static final DefaultRedisScript<Long> SOLD_OUT_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local countKey = KEYS[1]
                local totalKey = KEYS[2]
                local openAtKey = KEYS[3]
                local outAtKey = KEYS[4]
                local rankKey = KEYS[5]

                local ttlSeconds = tonumber(ARGV[1])
                local nowEpoch = tonumber(ARGV[2])
                local scheduleId = ARGV[3]
                local rankTtlSeconds = tonumber(ARGV[4])

                local count = redis.call('INCR', countKey)
                redis.call('EXPIRE', countKey, ttlSeconds)

                local total = tonumber(redis.call('GET', totalKey))
                local openAt = tonumber(redis.call('GET', openAtKey))
                if (total == nil or openAt == nil) then
                    return -1
                end

                if (count >= total) then
                    local set = redis.call('SETNX', outAtKey, nowEpoch)
                    if (set == 1) then
                        redis.call('EXPIRE', outAtKey, ttlSeconds)
                        local duration = nowEpoch - openAt
                        redis.call('ZADD', rankKey, duration, scheduleId)
                        redis.call('EXPIRE', rankKey, rankTtlSeconds)
                        return duration
                    end
                end

                return 0
                """);
        SOLD_OUT_SCRIPT = script;
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final SeatRepository seatRepository;
    private final ConcertScheduleRepository concertScheduleRepository;

    public void recordPayment(Long scheduleId, LocalDateTime paidAt) {
        ensureMeta(scheduleId);

        long nowEpochSeconds = paidAt.toEpochSecond(ZoneOffset.UTC);
        String dayKey = FastSoldOutRedisKeys.rankDay(DAY_FORMATTER.format(paidAt.toLocalDate()));

        List<String> keys = List.of(
                FastSoldOutRedisKeys.soldCount(scheduleId),
                FastSoldOutRedisKeys.soldTotal(scheduleId),
                FastSoldOutRedisKeys.soldOpenAt(scheduleId),
                FastSoldOutRedisKeys.soldOutAt(scheduleId),
                dayKey
        );

        Long result = stringRedisTemplate.execute(
                SOLD_OUT_SCRIPT,
                keys,
                String.valueOf(SOLD_TTL.getSeconds()),
                String.valueOf(nowEpochSeconds),
                String.valueOf(scheduleId),
                String.valueOf(RANK_TTL.getSeconds())
        );

        if (result == null) {
            log.warn("FastSoldOut redis script returned null. scheduleId={}", scheduleId);
            return;
        }

        if (result == -1) {
            log.warn("FastSoldOut meta missing in redis. scheduleId={}", scheduleId);
            return;
        }
    }

    private void ensureMeta(Long scheduleId) {
        String totalKey = FastSoldOutRedisKeys.soldTotal(scheduleId);
        String openAtKey = FastSoldOutRedisKeys.soldOpenAt(scheduleId);

        boolean totalExists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(totalKey));
        boolean openAtExists = Boolean.TRUE.equals(stringRedisTemplate.hasKey(openAtKey));

        if (totalExists && openAtExists) {
            return;
        }

        Optional<ConcertSchedule> scheduleOpt = concertScheduleRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            log.warn("ConcertSchedule not found for FastSoldOut meta. scheduleId={}", scheduleId);
            return;
        }

        ConcertSchedule schedule = scheduleOpt.get();
        long totalSeats = seatRepository.countBySchedule_ConcertScheduleId(scheduleId);
        if (totalSeats <= 0) {
            log.warn("Total seats is zero for FastSoldOut meta. scheduleId={}", scheduleId);
            return;
        }

        if (schedule.getSalesOpenAt() == null) {
            log.warn("Sales open time is null for FastSoldOut meta. scheduleId={}", scheduleId);
            return;
        }

        long openAtEpoch = schedule.getSalesOpenAt().toEpochSecond(ZoneOffset.UTC);

        if (!totalExists) {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    totalKey,
                    String.valueOf(totalSeats),
                    SOLD_TTL
            );
        }

        if (!openAtExists) {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    openAtKey,
                    String.valueOf(openAtEpoch),
                    SOLD_TTL
            );
        }
    }
}

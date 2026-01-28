package kr.hhplus.be.server.seat.servcie;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.config.cache.RedisLockService;
import kr.hhplus.be.server.common.exception.ConcertScheduleException;
import kr.hhplus.be.server.common.exception.DistributedLockException;
import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.common.exception.SeatException;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.domain.PaymentStatus;
import kr.hhplus.be.server.payment.repository.PaymentRepository;
import kr.hhplus.be.server.queue.service.QueueTokenService;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.dto.SeatDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto.SeatResponse;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final MemberRepository memberRepository;
    private final ConcertScheduleRepository concertScheduleRepository;
    private final PaymentRepository paymentRepository;
    private final QueueTokenService queueTokenService;
    private final RedisLockService redisLockService;

    public seatResponseDto getSeat(Long concertScheduleId) {

        List<SeatResponse> availableSeatList = seatRepository.findBySchedule_ConcertScheduleIdOrderBySeatNumberAsc(concertScheduleId)
                .stream()
                .peek(seat -> {
                    if (seat.getSeatStatus() == SeatStatus.LOCKED && seat.getLockedAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
                        seat.setSeatStatus(SeatStatus.AVAILABLE);
                        seat.setLockedUserId(null);
                        seat.setLockedAt(null);
                    }
                })
                .filter(seat -> seat.getSeatStatus() == SeatStatus.AVAILABLE)
                .map(e -> {
                    SeatResponse seat = new SeatResponse();
                    seat.setSeatId(e.getSeatId());
                    seat.setConcertScheduleId(e.getSchedule().getConcertScheduleId());

                    return seat;
                })
                .toList();

        return new seatResponseDto(availableSeatList);
    }

    /**
     * 좌석 예약 요청 (Redis 분산락 적용)
     * 
     * [적용 범위]
     * - 좌석 예약의 전체 트랜잭션 (좌석 상태 확인 → LOCKED 처리 → 결제 대기 생성)
     * 
     * [락 키 전략]
     * - "lock:seat:{concertScheduleId}:{seatId}"
     * - 콘서트 회차별, 좌석별로 독립적인 락 (동시성 극대화)
     * 
     * [분산락과 DB Tx 관계]
     * 1. 분산락 획득 (Redis SETNX)
     * 2. DB Transaction 시작 (@Transactional)
     * 3. 비즈니스 로직 수행
     * 4. DB Transaction 커밋
     * 5. 분산락 해제 (finally)
     * 
     * [타임아웃 설정]
     * - waitTime: 5초 (락 획득 대기)
     * - leaseTime: 10초 (락 자동 해제, 데드락 방지)
     */
    public void seatReservationRequest(SeatDto.seatReservationRequestDto requestDto, String queueToken, UserDetails userDetails) {
        // 적절한 락 키 생성: 콘서트 회차 + 좌석 ID
        String lockKey = RedisLockService.generateLockKey(
                "seat",
                requestDto.concertScheduleId(),
                requestDto.seatId()
        );

        log.info("좌석 예약 요청 - 분산락 시도: lockKey={}", lockKey);

        // 분산락 획득 (폴링 방식, 최대 5초 대기)
        boolean acquired = redisLockService.tryLock(lockKey, 5, 10);

        if (!acquired) {
            log.error("좌석 예약 실패 - 분산락 획득 실패: lockKey={}", lockKey);
            throw new DistributedLockException("다른 사용자가 해당 좌석을 예약 중입니다. 잠시 후 다시 시도해주세요.");
        }

        try {
            // 분산락 획득 후 DB Transaction 시작
            log.info("좌석 예약 처리 시작 - 분산락 획득 완료: lockKey={}", lockKey);
            seatReservationWithTransaction(requestDto, queueToken, userDetails);
            log.info("좌석 예약 처리 완료: lockKey={}", lockKey);
        } finally {
            // 반드시 분산락 해제 (데드락 방지)
            redisLockService.unlock(lockKey);
            log.info("분산락 해제: lockKey={}", lockKey);
        }
    }

    /**
     * 좌석 예약 비즈니스 로직 (DB Transaction 범위)
     * 
     * - 분산락 내부에서 실행되어야 함
     * - DB 락(PESSIMISTIC_WRITE) 대신 Redis 분산락으로 동시성 제어
     */
    @Transactional
    public void seatReservationWithTransaction(SeatDto.seatReservationRequestDto requestDto, String queueToken, UserDetails userDetails) {
        queueTokenService.validateActiveToken(queueToken, userDetails.getUsername());

        Member member = memberRepository.findByMemberId(userDetails.getUsername())
                .orElseThrow(InvalidUserException::new);

        Seat seat = seatRepository.findBySeatIdAndSchedule_ConcertScheduleId(requestDto.seatId(), requestDto.concertScheduleId())
                .orElseThrow(() -> new SeatException().NotExistsSeatException());

        if (seat.getSeatStatus() == SeatStatus.RESERVED) {
            throw new SeatException().ReservedSeatException();
        }

        // 예약된 좌석 > 만료 여부 확인
        if (seat.getSeatStatus() == SeatStatus.LOCKED && ObjectUtils.isNotEmpty(seat.getLockedAt())) {
            LocalDateTime now = LocalDateTime.now();
            boolean invalidLocked = seat.getLockedAt().plusMinutes(5).isAfter(now);

            if (invalidLocked) {
                throw new SeatException().ReservedSeatException();
            }
        }

        // 해당 좌석 LOCKED 처리
        seat.setSeatId(requestDto.seatId());
        seat.setSeatStatus(SeatStatus.LOCKED);
        seat.setLockedUserId(member.getId());
        seat.setLockedAt(LocalDateTime.now());

        seatRepository.save(seat);

        // 결제 대기 요청
        ConcertSchedule concertSchedule = concertScheduleRepository.findById(requestDto.concertScheduleId())
                .orElseThrow(() -> new ConcertScheduleException("유효하지 않은 콘서트 일정입니다."));

        Payment payment = new Payment();
        payment.setConcertId(concertSchedule.getConcert().getId());
        payment.setConcertScheduleId(requestDto.concertScheduleId());
        payment.setSeatId(requestDto.seatId());
        payment.setMemberId(member.getId());
        payment.setPrice(concertSchedule.getConcert().getPrice());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentAt(null);
        payment.setCancelReason(null);
        payment.setRegDt(LocalDateTime.now());
        payment.setModDt(LocalDateTime.now());

        paymentRepository.save(payment);
    }


}

package kr.hhplus.be.server.queue.service;

import kr.hhplus.be.server.queue.domain.*;
import kr.hhplus.be.server.queue.repository.QueueTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueTokenService {

    private final QueueTokenRepository queueTokenRepository;

    /**
     * 대기열 토큰 발급
     */
    @Transactional
    public String issueToken(String memberId) {

        QueueToken token = new QueueToken();
        token.setToken(UUID.randomUUID().toString());
        token.setMemberId(memberId);
        token.setStatus(QueueStatus.ACTIVE);
        token.setIssuedAt(LocalDateTime.now());
        token.setExpiredAt(LocalDateTime.now().plusMinutes(10));

        queueTokenRepository.save(token);

        return token.getToken();
    }

    /**
     * 좌석 예약 시 진입 시 검증
     */
    @Transactional
    public void validateActiveToken(String token, String memberId) {

        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("대기열 토큰이 존재하지 않습니다."));

        if (!queueToken.getMemberId().equals(memberId)) {
            throw new IllegalStateException("토큰 소유자가 아닙니다.");
        }

        if (queueToken.getStatus() != QueueStatus.ACTIVE) {
            throw new IllegalStateException("유효하지 않은 대기열 토큰입니다.");
        }

        if (queueToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            queueToken.setStatus(QueueStatus.EXPIRED);
            throw new IllegalStateException("대기열 토큰이 만료되었습니다.");
        }
    }

    /**
     * 결제 완료 후 토큰 사용 처리
     */
    @Transactional
    public void markUsed(String token) {

        QueueToken queueToken = queueTokenRepository.findByToken(token)
                .orElseThrow();

        queueToken.setStatus(QueueStatus.USED);
    }
}

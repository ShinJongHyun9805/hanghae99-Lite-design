package kr.hhplus.be.server.queue.repository;

import kr.hhplus.be.server.queue.domain.QueueToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QueueTokenRepository extends JpaRepository<QueueToken, Long> {

    Optional<QueueToken> findByToken(String token);

}

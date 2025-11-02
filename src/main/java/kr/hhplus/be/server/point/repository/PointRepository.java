package kr.hhplus.be.server.point.repository;

import kr.hhplus.be.server.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByMemberId(String memberId);
}

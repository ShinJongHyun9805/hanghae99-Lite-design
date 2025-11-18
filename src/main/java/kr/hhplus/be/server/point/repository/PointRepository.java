package kr.hhplus.be.server.point.repository;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByMemberId(String memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Point p where p.memberId = :memberId")
    Optional<Point> findForUpdateByMemberId(@Param("memberId") String memberId);
}

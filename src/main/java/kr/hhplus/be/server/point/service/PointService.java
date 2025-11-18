package kr.hhplus.be.server.point.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exception.ChargePointException;
import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.dto.PointDto;
import kr.hhplus.be.server.point.dto.PointDto.PointChargeRequest;
import kr.hhplus.be.server.point.dto.PointDto.pointResponse;
import kr.hhplus.be.server.point.repository.PointRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

    private final MemberRepository userRepository;

    @Transactional
    public pointResponse chargePoint(PointChargeRequest requestDto, UserDetails userDetails) {

        if (requestDto.amt() <= 0) {
            throw new ChargePointException("충전 금액은 1원 이상 가능합니다.");
        }

        Member member = userRepository.findForUpdateByMemberId(userDetails.getUsername())
                .orElseThrow(InvalidUserException::new);

        Point point = pointRepository.findForUpdateByMemberId(member.getMemberId())
                        .orElseGet(() -> {
                            Point np = new Point();
                            np.setMemberId(member.getMemberId());
                            np.setPointAmt(0);

                            return pointRepository.save(np);
                        });

        int next = point.getPointAmt() + requestDto.amt();
        if (next > Integer.MAX_VALUE) {
            throw new ChargePointException("충전 금액을 확인해 주세요.");
        }

        point.chargePoint(requestDto.amt());

        return new pointResponse(userDetails.getUsername(), point.getPointAmt());
    }

    public pointResponse getPoint(UserDetails userDetails) {

        Member member = userRepository.findByMemberId(userDetails.getUsername())
                .orElseThrow(InvalidUserException::new);

        int amt = pointRepository.findByMemberId(member.getMemberId())
                .map(Point::getPointAmt)
                .orElse(0);

        return new pointResponse(userDetails.getUsername(), amt);
    }
}

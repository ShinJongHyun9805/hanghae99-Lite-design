package kr.hhplus.be.server.point.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.repository.PointRepository;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final PointRepository pointRepository;

    private final MemberRepository userRepository;

    public PointChargeResponseDto chargePoint(PointChargeRequestDto requestDto) {

        Member member = userRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(InvalidUserException::new);

        Point point = pointRepository.findByMemberId(member.getMemberId())
                        .orElseGet(() -> {
                            Point newPoint = new Point();
                            newPoint.setMemberId(member.getMemberId());
                            newPoint.setPointAmt(0);

                            return newPoint;
                        });

        point.chargePoint(requestDto.getAmount());

        Point save = pointRepository.save(point);

        return new PointChargeResponseDto(save.getId(), save.getMemberId(), save.getPointAmt());
    }

    public PointChargeResponseDto getPoint(String memberId) {

        Member member = userRepository.findByMemberId(memberId)
                .orElseThrow(InvalidUserException::new);

        Point point = pointRepository.findByMemberId(member.getMemberId())
                .orElseThrow(InvalidUserException::new);

        return new PointChargeResponseDto(point.getId(), point.getMemberId(), point.getPointAmt());
    }
}

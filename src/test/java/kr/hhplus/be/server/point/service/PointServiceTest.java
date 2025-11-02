package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private MemberRepository userRepository;

    @InjectMocks
    private PointService pointService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 존재하지_않는_회원이면_InvalidUserException_발생() {

        PointChargeRequestDto dto = new PointChargeRequestDto();
        dto.setMemberId("shin");
        dto.setAmount(1000);

        given(userRepository.findByMemberId("shin")).willReturn(Optional.empty());

        assertThrows(InvalidUserException.class, () -> pointService.chargePoint(dto));

        verify(userRepository, times(1)).findByMemberId("shin");
        verify(pointRepository, never()).save(any());
    }

    @Test
    void 기존_포인트가_있으면_금액을_추가하고_저장() {

        Member member = new Member();
        member.setMemberId("shin");

        Point point = new Point();
        point.setMemberId("shin");
        point.setPointAmt(1000);

        PointChargeRequestDto dto = new PointChargeRequestDto();
        dto.setMemberId("shin");
        dto.setAmount(500);

        given(userRepository.findByMemberId("shin")).willReturn(Optional.of(member));
        given(pointRepository.findByMemberId("shin")).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        PointChargeResponseDto pointChargeResponseDto = pointService.chargePoint(dto);

        assertEquals(1500, pointChargeResponseDto.getPointAmt());
        verify(pointRepository, times(1)).save(point);
    }

    @Test
    void 포인트_정보가_없으면_새_Point를_생성하여_저장() {

        Member member = new Member();
        member.setMemberId("shin");

        PointChargeRequestDto dto = new PointChargeRequestDto();
        dto.setMemberId("shin");
        dto.setAmount(300);

        given(userRepository.findByMemberId("shin")).willReturn(Optional.of(member));
        given(pointRepository.findByMemberId("shin")).willReturn(Optional.empty());
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        PointChargeResponseDto pointChargeResponseDto = pointService.chargePoint(dto);

        assertEquals("shin", pointChargeResponseDto.getMemberId());
        assertEquals(300, pointChargeResponseDto.getPointAmt());
        verify(pointRepository, times(1)).save(any(Point.class));
    }


}